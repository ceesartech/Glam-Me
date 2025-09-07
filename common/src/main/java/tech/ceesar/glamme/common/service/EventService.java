package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.event.EventPublisher;

import java.util.Map;

/**
 * Generic event service for publishing domain events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventPublisher eventPublisher;

    /**
     * Publish a user event
     */
    public void publishUserEvent(String eventType, String userId, Map<String, Object> details) {
        try {
            eventPublisher.publishEvent("user." + eventType, Map.of(
                "userId", userId,
                "eventType", eventType,
                "details", details
            ), Map.of("service", "matching-service"));
        } catch (Exception e) {
            log.error("Failed to publish user event: {} for user: {}", eventType, userId, e);
        }
    }

    /**
     * Publish a stylist event
     */
    public void publishStylistEvent(String eventType, String stylistId, Map<String, Object> details) {
        try {
            eventPublisher.publishEvent("stylist." + eventType, Map.of(
                "stylistId", stylistId,
                "eventType", eventType,
                "details", details
            ), Map.of("service", "matching-service"));
        } catch (Exception e) {
            log.error("Failed to publish stylist event: {} for stylist: {}", eventType, stylistId, e);
        }
    }

    /**
     * Publish a match event
     */
    public void publishMatchEvent(String eventType, Long matchId, String customerId, String stylistId, Map<String, Object> details) {
        try {
            eventPublisher.publishEvent("match." + eventType, Map.of(
                "matchId", matchId,
                "customerId", customerId,
                "stylistId", stylistId,
                "eventType", eventType,
                "details", details
            ), Map.of("service", "matching-service"));
        } catch (Exception e) {
            log.error("Failed to publish match event: {} for match: {}", eventType, matchId, e);
        }
    }

    /**
     * Publish a generic event
     */
    public void publishEvent(String eventType, Map<String, Object> data) {
        try {
            eventPublisher.publishEvent(eventType, data, Map.of("service", "matching-service"));
        } catch (Exception e) {
            log.error("Failed to publish event: {}", eventType, e);
        }
    }

    /**
     * Publish an event with bus name, source, and data
     */
    public void publishEvent(String busName, String source, String eventType, Map<String, Object> data) {
        try {
            // For EventBridge, the event type becomes the detail-type
            Map<String, Object> eventData = new java.util.HashMap<>(data);
            eventData.put("source", source);
            eventPublisher.publishEvent(eventType, eventData, Map.of("service", source));
        } catch (Exception e) {
            log.error("Failed to publish event: {} from source: {}", eventType, source, e);
        }
    }
}
