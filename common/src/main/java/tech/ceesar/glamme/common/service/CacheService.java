package tech.ceesar.glamme.common.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Generic cache service interface
 */
public interface CacheService {

    /**
     * Get a value from cache
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Put a value in cache with default TTL
     */
    void set(String key, Object value);

    /**
     * Put a value in cache with custom TTL
     */
    void set(String key, Object value, Duration ttl);

    /**
     * Delete a value from cache
     */
    boolean delete(String key);

    /**
     * Check if key exists in cache
     */
    boolean exists(String key);

    /**
     * Get cache key with TTL
     */
    Duration getTtl(String key);

    /**
     * Increment a numeric value
     */
    Long increment(String key);

    /**
     * Decrement a numeric value
     */
    Long decrement(String key);
}
