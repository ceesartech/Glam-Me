package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if request is allowed based on rate limit
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        try {
            String redisKey = "ratelimit:" + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowSeconds = window.getSeconds();

            // Use Redis sorted set to track requests within the time window
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);

            // Remove old entries outside the time window
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, currentTime - windowSeconds);

            // Set expiration on the key
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);

            // Count requests in current window
            Long requestCount = redisTemplate.opsForZSet().zCard(redisKey);

            boolean allowed = requestCount != null && requestCount <= maxRequests;

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {}, requests: {}/{}", key, requestCount, maxRequests);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Allow request on error to avoid blocking legitimate traffic
            return true;
        }
    }

    /**
     * Rate limit by IP address
     */
    public boolean isAllowedByIp(String ipAddress, int maxRequests, Duration window) {
        return isAllowed("ip:" + ipAddress, maxRequests, window);
    }

    /**
     * Rate limit by user ID
     */
    public boolean isAllowedByUser(String userId, int maxRequests, Duration window) {
        return isAllowed("user:" + userId, maxRequests, window);
    }

    /**
     * Rate limit by endpoint
     */
    public boolean isAllowedByEndpoint(String endpoint, int maxRequests, Duration window) {
        return isAllowed("endpoint:" + endpoint, maxRequests, window);
    }

    /**
     * Rate limit by API key
     */
    public boolean isAllowedByApiKey(String apiKey, int maxRequests, Duration window) {
        return isAllowed("apikey:" + apiKey, maxRequests, window);
    }

    /**
     * Get remaining requests for a key
     */
    public long getRemainingRequests(String key, int maxRequests, Duration window) {
        try {
            String redisKey = "ratelimit:" + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowSeconds = window.getSeconds();

            // Clean up old entries
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, currentTime - windowSeconds);

            Long requestCount = redisTemplate.opsForZSet().zCard(redisKey);
            long count = requestCount != null ? requestCount : 0;

            return Math.max(0, maxRequests - count);

        } catch (Exception e) {
            log.error("Error getting remaining requests for key: {}", key, e);
            return maxRequests;
        }
    }

    /**
     * Get time until reset for a key
     */
    public Duration getTimeUntilReset(String key, Duration window) {
        try {
            String redisKey = "ratelimit:" + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowSeconds = window.getSeconds();

            // Get the oldest timestamp in the current window
            Object oldestTimestamp = redisTemplate.opsForZSet().range(redisKey, 0, 0).stream().findFirst().orElse(null);

            if (oldestTimestamp != null) {
                long oldest = Long.parseLong(oldestTimestamp.toString());
                long resetTime = oldest + windowSeconds;
                long secondsUntilReset = resetTime - currentTime;
                return Duration.ofSeconds(Math.max(0, secondsUntilReset));
            }

            return Duration.ZERO;

        } catch (Exception e) {
            log.error("Error getting time until reset for key: {}", key, e);
            return Duration.ZERO;
        }
    }

    /**
     * Clear rate limit for a key
     */
    public void clearRateLimit(String key) {
        try {
            String redisKey = "ratelimit:" + key;
            redisTemplate.delete(redisKey);
            log.info("Cleared rate limit for key: {}", key);
        } catch (Exception e) {
            log.error("Error clearing rate limit for key: {}", key, e);
        }
    }

    /**
     * Common rate limit configurations
     */
    public static class RateLimits {
        // API endpoints
        public static final int API_MAX_REQUESTS = 100;
        public static final Duration API_WINDOW = Duration.ofMinutes(1);

        // Authentication endpoints
        public static final int AUTH_MAX_REQUESTS = 5;
        public static final Duration AUTH_WINDOW = Duration.ofMinutes(15);

        // Image processing
        public static final int IMAGE_MAX_REQUESTS = 10;
        public static final Duration IMAGE_WINDOW = Duration.ofMinutes(1);

        // Search endpoints
        public static final int SEARCH_MAX_REQUESTS = 50;
        public static final Duration SEARCH_WINDOW = Duration.ofMinutes(1);

        // Booking endpoints
        public static final int BOOKING_MAX_REQUESTS = 20;
        public static final Duration BOOKING_WINDOW = Duration.ofMinutes(1);

        // Shopping endpoints
        public static final int SHOPPING_MAX_REQUESTS = 30;
        public static final Duration SHOPPING_WINDOW = Duration.ofMinutes(1);

        // Per user limits
        public static final int USER_MAX_REQUESTS = 1000;
        public static final Duration USER_WINDOW = Duration.ofHours(1);

        // Per IP limits (more restrictive)
        public static final int IP_MAX_REQUESTS = 100;
        public static final Duration IP_WINDOW = Duration.ofMinutes(1);
    }

    /**
     * Check API endpoint rate limit
     */
    public boolean checkApiRateLimit(String endpoint, String userId, String ipAddress) {
        // Check user limit (most permissive)
        if (!isAllowedByUser(userId, RateLimits.USER_MAX_REQUESTS, RateLimits.USER_WINDOW)) {
            return false;
        }

        // Check IP limit (more restrictive)
        if (!isAllowedByIp(ipAddress, RateLimits.IP_MAX_REQUESTS, RateLimits.IP_WINDOW)) {
            return false;
        }

        // Check endpoint specific limit
        return isAllowedByEndpoint(endpoint, RateLimits.API_MAX_REQUESTS, RateLimits.API_WINDOW);
    }

    /**
     * Check authentication rate limit
     */
    public boolean checkAuthRateLimit(String identifier) {
        return isAllowed("auth:" + identifier, RateLimits.AUTH_MAX_REQUESTS, RateLimits.AUTH_WINDOW);
    }

    /**
     * Check image processing rate limit
     */
    public boolean checkImageRateLimit(String userId) {
        return isAllowedByUser(userId, RateLimits.IMAGE_MAX_REQUESTS, RateLimits.IMAGE_WINDOW);
    }

    /**
     * Check search rate limit
     */
    public boolean checkSearchRateLimit(String userId) {
        return isAllowedByUser(userId, RateLimits.SEARCH_MAX_REQUESTS, RateLimits.SEARCH_WINDOW);
    }

    /**
     * Check booking rate limit
     */
    public boolean checkBookingRateLimit(String userId) {
        return isAllowedByUser(userId, RateLimits.BOOKING_MAX_REQUESTS, RateLimits.BOOKING_WINDOW);
    }

    /**
     * Check shopping rate limit
     */
    public boolean checkShoppingRateLimit(String userId) {
        return isAllowedByUser(userId, RateLimits.SHOPPING_MAX_REQUESTS, RateLimits.SHOPPING_WINDOW);
    }
}
