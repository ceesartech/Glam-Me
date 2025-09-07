package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Acquire a distributed lock
     */
    public boolean acquireLock(String lockKey, String lockValue, Duration lockDuration) {
        try {
            String key = "lock:" + lockKey;
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, lockValue, lockDuration);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired lock: {} with value: {}", lockKey, lockValue);
                return true;
            } else {
                log.debug("Failed to acquire lock: {} (already held)", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Error acquiring lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Release a distributed lock
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            String key = "lock:" + lockKey;
            String currentValue = (String) redisTemplate.opsForValue().get(key);

            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(key);
                log.debug("Released lock: {}", lockKey);
                return true;
            } else {
                log.warn("Lock value mismatch for key: {} (expected: {}, actual: {})", lockKey, lockValue, currentValue);
                return false;
            }
        } catch (Exception e) {
            log.error("Error releasing lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Check if lock is held
     */
    public boolean isLocked(String lockKey) {
        try {
            String key = "lock:" + lockKey;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking lock status: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Extend lock duration
     */
    public boolean extendLock(String lockKey, String lockValue, Duration newDuration) {
        try {
            String key = "lock:" + lockKey;
            String currentValue = (String) redisTemplate.opsForValue().get(key);

            if (lockValue.equals(currentValue)) {
                redisTemplate.expire(key, newDuration.getSeconds(), TimeUnit.SECONDS);
                log.debug("Extended lock: {} to {}s", lockKey, newDuration.getSeconds());
                return true;
            } else {
                log.warn("Cannot extend lock - value mismatch for key: {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Error extending lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Force release lock (use with caution)
     */
    public void forceReleaseLock(String lockKey) {
        try {
            String key = "lock:" + lockKey;
            redisTemplate.delete(key);
            log.warn("Force released lock: {}", lockKey);
        } catch (Exception e) {
            log.error("Error force releasing lock: {}", lockKey, e);
        }
    }

    /**
     * Execute with lock (template method)
     */
    public <T> T executeWithLock(String lockKey, String lockValue, Duration lockDuration,
                                LockOperation<T> operation, T defaultValue) {
        if (!acquireLock(lockKey, lockValue, lockDuration)) {
            log.warn("Could not acquire lock for operation: {}", lockKey);
            return defaultValue;
        }

        try {
            return operation.execute();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * Execute with lock (Runnable version)
     */
    public void executeWithLock(String lockKey, String lockValue, Duration lockDuration,
                               Runnable operation) {
        if (!acquireLock(lockKey, lockValue, lockDuration)) {
            log.warn("Could not acquire lock for operation: {}", lockKey);
            return;
        }

        try {
            operation.run();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @FunctionalInterface
    public interface LockOperation<T> {
        T execute();
    }

    /**
     * User-specific locks
     */
    public boolean acquireUserLock(String userId, String operation) {
        String lockKey = "user:" + userId + ":" + operation;
        String lockValue = userId + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofMinutes(5));
    }

    public void releaseUserLock(String userId, String operation, String lockValue) {
        String lockKey = "user:" + userId + ":" + operation;
        releaseLock(lockKey, lockValue);
    }

    /**
     * Resource-specific locks
     */
    public boolean acquireResourceLock(String resourceType, String resourceId) {
        String lockKey = "resource:" + resourceType + ":" + resourceId;
        String lockValue = resourceType + ":" + resourceId + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofMinutes(2));
    }

    public void releaseResourceLock(String resourceType, String resourceId, String lockValue) {
        String lockKey = "resource:" + resourceType + ":" + resourceId;
        releaseLock(lockKey, lockValue);
    }

    /**
     * Stylist-specific locks
     */
    public boolean acquireStylistLock(String stylistId, String operation) {
        String lockKey = "stylist:" + stylistId + ":" + operation;
        String lockValue = stylistId + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofMinutes(3));
    }

    public void releaseStylistLock(String stylistId, String operation, String lockValue) {
        String lockKey = "stylist:" + stylistId + ":" + operation;
        releaseLock(lockKey, lockValue);
    }

    /**
     * Booking-specific locks
     */
    public boolean acquireBookingLock(String stylistId, String timeSlot) {
        String lockKey = "booking:" + stylistId + ":" + timeSlot;
        String lockValue = stylistId + ":" + timeSlot + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofMinutes(1));
    }

    public void releaseBookingLock(String stylistId, String timeSlot, String lockValue) {
        String lockKey = "booking:" + stylistId + ":" + timeSlot;
        releaseLock(lockKey, lockValue);
    }

    /**
     * Payment-specific locks
     */
    public boolean acquirePaymentLock(String paymentId) {
        String lockKey = "payment:" + paymentId;
        String lockValue = paymentId + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofMinutes(5));
    }

    public void releasePaymentLock(String paymentId, String lockValue) {
        String lockKey = "payment:" + paymentId;
        releaseLock(lockKey, lockValue);
    }

    /**
     * Inventory locks for shopping
     */
    public boolean acquireInventoryLock(String productId) {
        String lockKey = "inventory:" + productId;
        String lockValue = productId + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofSeconds(30));
    }

    public void releaseInventoryLock(String productId, String lockValue) {
        String lockKey = "inventory:" + productId;
        releaseLock(lockKey, lockValue);
    }

    /**
     * Image processing locks
     */
    public boolean acquireImageProcessingLock(String imageId) {
        String lockKey = "image:processing:" + imageId;
        String lockValue = imageId + ":" + System.currentTimeMillis();
        return acquireLock(lockKey, lockValue, Duration.ofMinutes(10));
    }

    public void releaseImageProcessingLock(String imageId, String lockValue) {
        String lockKey = "image:processing:" + imageId;
        releaseLock(lockKey, lockValue);
    }
}
