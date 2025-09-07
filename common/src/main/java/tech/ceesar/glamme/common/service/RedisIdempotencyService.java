package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if operation is already in progress or completed
     */
    public boolean isOperationInProgress(String operationId) {
        try {
            String key = "idempotency:" + operationId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking idempotency for operation: {}", operationId, e);
            return false;
        }
    }

    /**
     * Start an idempotent operation
     */
    public boolean startOperation(String operationId, Object operationData, Duration ttl) {
        try {
            String key = "idempotency:" + operationId;
            String statusKey = "idempotency:status:" + operationId;
            String dataKey = "idempotency:data:" + operationId;

            // Check if operation is already in progress
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                log.warn("Operation already in progress: {}", operationId);
                return false;
            }

            // Start the operation
            redisTemplate.opsForValue().set(key, "IN_PROGRESS", ttl);
            redisTemplate.opsForValue().set(statusKey, "IN_PROGRESS", ttl);
            if (operationData != null) {
                redisTemplate.opsForValue().set(dataKey, operationData, ttl);
            }

            log.info("Started idempotent operation: {}", operationId);
            return true;

        } catch (Exception e) {
            log.error("Error starting idempotent operation: {}", operationId, e);
            return false;
        }
    }

    /**
     * Complete an idempotent operation
     */
    public void completeOperation(String operationId, Object result) {
        try {
            String key = "idempotency:" + operationId;
            String statusKey = "idempotency:status:" + operationId;
            String resultKey = "idempotency:result:" + operationId;

            redisTemplate.opsForValue().set(key, "COMPLETED");
            redisTemplate.opsForValue().set(statusKey, "COMPLETED");
            if (result != null) {
                redisTemplate.opsForValue().set(resultKey, result);
            }

            // Extend TTL for completed operations (keep result for 24 hours)
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            redisTemplate.expire(statusKey, 24, TimeUnit.HOURS);
            redisTemplate.expire(resultKey, 24, TimeUnit.HOURS);

            log.info("Completed idempotent operation: {}", operationId);

        } catch (Exception e) {
            log.error("Error completing idempotent operation: {}", operationId, e);
        }
    }

    /**
     * Fail an idempotent operation
     */
    public void failOperation(String operationId, String errorMessage) {
        try {
            String key = "idempotency:" + operationId;
            String statusKey = "idempotency:status:" + operationId;
            String errorKey = "idempotency:error:" + operationId;

            redisTemplate.opsForValue().set(key, "FAILED");
            redisTemplate.opsForValue().set(statusKey, "FAILED");
            if (errorMessage != null) {
                redisTemplate.opsForValue().set(errorKey, errorMessage);
            }

            // Keep failed operations for shorter time (1 hour)
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            redisTemplate.expire(statusKey, 1, TimeUnit.HOURS);
            redisTemplate.expire(errorKey, 1, TimeUnit.HOURS);

            log.info("Failed idempotent operation: {}", operationId);

        } catch (Exception e) {
            log.error("Error failing idempotent operation: {}", operationId, e);
        }
    }

    /**
     * Get operation status
     */
    public Optional<String> getOperationStatus(String operationId) {
        try {
            String statusKey = "idempotency:status:" + operationId;
            Object status = redisTemplate.opsForValue().get(statusKey);
            return Optional.ofNullable(status != null ? status.toString() : null);
        } catch (Exception e) {
            log.error("Error getting operation status: {}", operationId, e);
            return Optional.empty();
        }
    }

    /**
     * Get operation result
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOperationResult(String operationId, Class<T> resultType) {
        try {
            String resultKey = "idempotency:result:" + operationId;
            Object result = redisTemplate.opsForValue().get(resultKey);
            if (result != null) {
                if (resultType.isInstance(result)) {
                    return Optional.of((T) result);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting operation result: {}", operationId, e);
            return Optional.empty();
        }
    }

    /**
     * Get operation data
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOperationData(String operationId, Class<T> dataType) {
        try {
            String dataKey = "idempotency:data:" + operationId;
            Object data = redisTemplate.opsForValue().get(dataKey);
            if (data != null) {
                if (dataType.isInstance(data)) {
                    return Optional.of((T) data);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting operation data: {}", operationId, e);
            return Optional.empty();
        }
    }

    /**
     * Clean up expired idempotency keys
     */
    public void cleanupExpiredOperations() {
        try {
            // This is mainly handled by Redis TTL, but we can add custom cleanup logic if needed
            log.info("Idempotency cleanup completed");
        } catch (Exception e) {
            log.error("Error during idempotency cleanup", e);
        }
    }

    /**
     * Generate idempotency key for payment operations
     */
    public String generatePaymentIdempotencyKey(String userId, String paymentIntentId) {
        return "payment:" + userId + ":" + paymentIntentId;
    }

    /**
     * Generate idempotency key for booking operations
     */
    public String generateBookingIdempotencyKey(String userId, String stylistId, String startTime) {
        return "booking:" + userId + ":" + stylistId + ":" + startTime;
    }

    /**
     * Generate idempotency key for order operations
     */
    public String generateOrderIdempotencyKey(String userId, String cartId) {
        return "order:" + userId + ":" + cartId;
    }

    /**
     * Generate idempotency key for image processing
     */
    public String generateImageProcessingIdempotencyKey(String userId, String imageKey) {
        return "image:" + userId + ":" + imageKey;
    }

    /**
     * Generate idempotency key for ride operations
     */
    public String generateRideIdempotencyKey(String customerId, String pickupLocation, String dropoffLocation) {
        return "ride:" + customerId + ":" + pickupLocation + ":" + dropoffLocation;
    }

    /**
     * Start ride operation with idempotency check
     */
    public boolean startRideOperation(String customerId, String idempotencyKey, Object rideData) {
        return startOperation(idempotencyKey, rideData, Duration.ofMinutes(30));
    }

    /**
     * Check payment idempotency
     */
    public boolean checkPaymentIdempotency(String userId, String paymentIntentId) {
        String key = generatePaymentIdempotencyKey(userId, paymentIntentId);
        return !isOperationInProgress(key);
    }

    /**
     * Check booking idempotency
     */
    public boolean checkBookingIdempotency(String userId, String stylistId, String startTime) {
        String key = generateBookingIdempotencyKey(userId, stylistId, startTime);
        return !isOperationInProgress(key);
    }

    /**
     * Check order idempotency
     */
    public boolean checkOrderIdempotency(String userId, String cartId) {
        String key = generateOrderIdempotencyKey(userId, cartId);
        return !isOperationInProgress(key);
    }

    /**
     * Check image processing idempotency
     */
    public boolean checkImageProcessingIdempotency(String userId, String imageKey) {
        String key = generateImageProcessingIdempotencyKey(userId, imageKey);
        return !isOperationInProgress(key);
    }

    /**
     * Start payment operation
     */
    public boolean startPaymentOperation(String userId, String paymentIntentId, Object paymentData) {
        String key = generatePaymentIdempotencyKey(userId, paymentIntentId);
        return startOperation(key, paymentData, Duration.ofHours(24));
    }

    /**
     * Start booking operation
     */
    public boolean startBookingOperation(String userId, String stylistId, String startTime, Object bookingData) {
        String key = generateBookingIdempotencyKey(userId, stylistId, startTime);
        return startOperation(key, bookingData, Duration.ofHours(1));
    }

    /**
     * Start order operation
     */
    public boolean startOrderOperation(String userId, String cartId, Object orderData) {
        String key = generateOrderIdempotencyKey(userId, cartId);
        return startOperation(key, orderData, Duration.ofHours(1));
    }

    /**
     * Start image processing operation
     */
    public boolean startImageProcessingOperation(String userId, String imageKey, Object imageData) {
        String key = generateImageProcessingIdempotencyKey(userId, imageKey);
        return startOperation(key, imageData, Duration.ofHours(1));
    }
}
