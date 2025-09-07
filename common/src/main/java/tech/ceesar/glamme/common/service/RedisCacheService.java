package tech.ceesar.glamme.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cache a value with default TTL (1 hour)
     */
    public void set(String key, Object value) {
        set(key, value, Duration.ofHours(1));
    }

    /**
     * Cache a value with custom TTL
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached key: {} with TTL: {}s", key, ttl.getSeconds());
        } catch (Exception e) {
            log.error("Failed to cache key: {}", key, e);
        }
    }

    /**
     * Get cached value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                if (type.isInstance(value)) {
                    return Optional.of((T) value);
                } else {
                    // Try to deserialize if it's a JSON string
                    String jsonValue = value.toString();
                    T deserialized = objectMapper.readValue(jsonValue, type);
                    return Optional.of(deserialized);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get cached value for key: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * Delete cached value
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cache key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete cache key: {}", key, e);
        }
    }

    /**
     * Check if key exists
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check if key exists: {}", key, e);
            return false;
        }
    }

    /**
     * Set TTL for existing key
     */
    public void expire(String key, Duration ttl) {
        try {
            redisTemplate.expire(key, ttl.getSeconds(), TimeUnit.SECONDS);
            log.debug("Set TTL for key: {} to {}s", key, ttl.getSeconds());
        } catch (Exception e) {
            log.error("Failed to set TTL for key: {}", key, e);
        }
    }

    /**
     * Increment counter
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment key: {}", key, e);
            return 0L;
        }
    }

    /**
     * Add to set
     */
    public void addToSet(String key, Object... values) {
        try {
            redisTemplate.opsForSet().add(key, values);
            log.debug("Added {} items to set: {}", values.length, key);
        } catch (Exception e) {
            log.error("Failed to add to set: {}", key, e);
        }
    }

    /**
     * Get set members
     */
    public Set<Object> getSetMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Failed to get set members for key: {}", key, e);
            return Set.of();
        }
    }

    /**
     * Remove from set
     */
    public void removeFromSet(String key, Object value) {
        try {
            redisTemplate.opsForSet().remove(key, value);
            log.debug("Removed item from set: {}", key);
        } catch (Exception e) {
            log.error("Failed to remove from set: {}", key, e);
        }
    }

    /**
     * Cache user session data
     */
    public void cacheUserSession(String userId, Object sessionData, Duration ttl) {
        String key = "session:" + userId;
        set(key, sessionData, ttl);
    }

    /**
     * Get user session data
     */
    public <T> Optional<T> getUserSession(String userId, Class<T> type) {
        String key = "session:" + userId;
        return get(key, type);
    }

    /**
     * Cache API response
     */
    public void cacheApiResponse(String endpoint, String params, Object response, Duration ttl) {
        String key = "api:" + endpoint + ":" + params.hashCode();
        set(key, response, ttl);
    }

    /**
     * Get cached API response
     */
    public <T> Optional<T> getCachedApiResponse(String endpoint, String params, Class<T> type) {
        String key = "api:" + endpoint + ":" + params.hashCode();
        return get(key, type);
    }

    /**
     * Cache stylist data
     */
    public void cacheStylistData(String stylistId, Object stylistData) {
        String key = "stylist:" + stylistId;
        set(key, stylistData, Duration.ofHours(6)); // Cache for 6 hours
    }

    /**
     * Get cached stylist data
     */
    public <T> Optional<T> getCachedStylistData(String stylistId, Class<T> type) {
        String key = "stylist:" + stylistId;
        return get(key, type);
    }

    /**
     * Cache product data
     */
    public void cacheProductData(String productId, Object productData) {
        String key = "product:" + productId;
        set(key, productData, Duration.ofHours(12)); // Cache for 12 hours
    }

    /**
     * Get cached product data
     */
    public <T> Optional<T> getCachedProductData(String productId, Class<T> type) {
        String key = "product:" + productId;
        return get(key, type);
    }

    /**
     * Clear all cache for a pattern
     */
    public void clearCachePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Failed to clear cache pattern: {}", pattern, e);
        }
    }

    /**
     * Clear all user-related cache
     */
    public void clearUserCache(String userId) {
        clearCachePattern("session:" + userId);
        clearCachePattern("user:" + userId + "*");
    }

    /**
     * Clear all stylist-related cache
     */
    public void clearStylistCache(String stylistId) {
        clearCachePattern("stylist:" + stylistId);
        clearCachePattern("matching:stylist:" + stylistId + "*");
    }
}
