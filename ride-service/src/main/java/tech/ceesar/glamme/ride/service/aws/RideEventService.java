package tech.ceesar.glamme.ride.service.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import tech.ceesar.glamme.common.event.EventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideEventService {

    private final EventBridgeClient eventBridgeClient;
    private final EventPublisher eventPublisher;

    @Value("${aws.eventbridge.bus-name:glamme-bus}")
    private String eventBusName;

    /**
     * Publish ride requested event
     */
    public void publishRideRequested(String rideId, String customerId, String provider,
                                   Double pickupLat, Double pickupLng,
                                   Double dropoffLat, Double dropoffLng) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "customerId", customerId,
                "provider", provider,
                "pickupLocation", Map.of(
                        "latitude", pickupLat,
                        "longitude", pickupLng
                ),
                "dropoffLocation", Map.of(
                        "latitude", dropoffLat,
                        "longitude", dropoffLng
                ),
                "requestedAt", Instant.now().toString(),
                "status", "REQUESTED"
        );

        publishEvent("ride.requested", eventDetail);
        log.info("Published ride requested event for ride: {}", rideId);
    }

    /**
     * Publish ride accepted event
     */
    public void publishRideAccepted(String rideId, String driverId, String driverName,
                                  String vehicleModel, String vehicleLicense, Integer etaMinutes) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "driverId", driverId,
                "driverName", driverName,
                "vehicleModel", vehicleModel,
                "vehicleLicense", vehicleLicense,
                "etaMinutes", etaMinutes,
                "acceptedAt", Instant.now().toString(),
                "status", "ACCEPTED"
        );

        publishEvent("ride.accepted", eventDetail);
        log.info("Published ride accepted event for ride: {}", rideId);
    }

    /**
     * Publish ride started event
     */
    public void publishRideStarted(String rideId, Double startLat, Double startLng) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "startLocation", Map.of(
                        "latitude", startLat,
                        "longitude", startLng
                ),
                "startedAt", Instant.now().toString(),
                "status", "IN_PROGRESS"
        );

        publishEvent("ride.started", eventDetail);
        log.info("Published ride started event for ride: {}", rideId);
    }

    /**
     * Publish ride completed event
     */
    public void publishRideCompleted(String rideId, Double distance, Integer durationMinutes,
                                   Double fare, String currency) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "distance", distance,
                "durationMinutes", durationMinutes,
                "fare", fare,
                "currency", currency,
                "completedAt", Instant.now().toString(),
                "status", "COMPLETED"
        );

        publishEvent("ride.completed", eventDetail);
        log.info("Published ride completed event for ride: {}", rideId);
    }

    /**
     * Publish ride cancelled event
     */
    public void publishRideCancelled(String rideId, String cancelledBy, String reason) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "cancelledBy", cancelledBy,
                "reason", reason,
                "cancelledAt", Instant.now().toString(),
                "status", "CANCELLED"
        );

        publishEvent("ride.cancelled", eventDetail);
        log.info("Published ride cancelled event for ride: {}", rideId);
    }

    /**
     * Publish driver location update
     */
    public void publishDriverLocationUpdate(String driverId, Double latitude, Double longitude,
                                          String rideId, Boolean available) {

        Map<String, Object> eventDetail = Map.of(
                "driverId", driverId,
                "location", Map.of(
                        "latitude", latitude,
                        "longitude", longitude
                ),
                "rideId", rideId,
                "available", available,
                "timestamp", Instant.now().toString()
        );

        publishEvent("driver.location.updated", eventDetail);
        log.debug("Published driver location update for driver: {}", driverId);
    }

    /**
     * Publish payment processed event
     */
    public void publishPaymentProcessed(String rideId, String paymentId, Double amount,
                                      String currency, String status) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "paymentId", paymentId,
                "amount", amount,
                "currency", currency,
                "status", status,
                "processedAt", Instant.now().toString()
        );

        publishEvent("ride.payment.processed", eventDetail);
        log.info("Published payment processed event for ride: {}", rideId);
    }

    /**
     * Publish ride issue/reported event
     */
    public void publishRideIssueReported(String rideId, String issueType, String description,
                                       String reportedBy) {

        Map<String, Object> eventDetail = Map.of(
                "rideId", rideId,
                "issueType", issueType,
                "description", description,
                "reportedBy", reportedBy,
                "reportedAt", Instant.now().toString()
        );

        publishEvent("ride.issue.reported", eventDetail);
        log.info("Published ride issue reported event for ride: {}", rideId);
    }

    /**
     * Generic event publishing method
     */
    private void publishEvent(String eventType, Map<String, Object> eventDetail) {
        try {
            PutEventsRequestEntry eventEntry = PutEventsRequestEntry.builder()
                    .source("ride-service")
                    .detailType(eventType)
                    .detail(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(eventDetail))
                    .eventBusName(eventBusName)
                    .time(Instant.now())
                    .build();

            PutEventsRequest request = PutEventsRequest.builder()
                    .entries(eventEntry)
                    .build();

            var response = eventBridgeClient.putEvents(request);

            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish {} event entries", response.failedEntryCount());
                response.entries().forEach(entry -> {
                    if (entry.errorCode() != null) {
                        log.error("EventBridge error: {} - {}", entry.errorCode(), entry.errorMessage());
                    }
                });
            } else {
                log.debug("Successfully published {} event to EventBridge", eventType);
            }

        } catch (Exception e) {
            log.error("Failed to publish event: {}", eventType, e);
            // Fallback to common EventPublisher if EventBridge fails
            try {
                eventPublisher.publishEvent(eventType, eventDetail);
            } catch (Exception fallbackError) {
                log.error("Fallback event publishing also failed", fallbackError);
            }
        }
    }

    /**
     * Publish ride telemetry data (for analytics)
     */
    public void publishRideTelemetry(String rideId, String driverId, Double speed,
                                   Double latitude, Double longitude, Integer passengerCount) {

        Map<String, Object> telemetryData = Map.of(
                "rideId", rideId,
                "driverId", driverId,
                "speed", speed,
                "location", Map.of(
                        "latitude", latitude,
                        "longitude", longitude
                ),
                "passengerCount", passengerCount,
                "timestamp", Instant.now().toString()
        );

        publishEvent("ride.telemetry", telemetryData);
        log.debug("Published ride telemetry for ride: {}", rideId);
    }

    /**
     * Publish driver availability status
     */
    public void publishDriverAvailability(String driverId, Boolean available,
                                        Double latitude, Double longitude) {

        Map<String, Object> availabilityData = Map.of(
                "driverId", driverId,
                "available", available,
                "location", Map.of(
                        "latitude", latitude,
                        "longitude", longitude
                ),
                "timestamp", Instant.now().toString()
        );

        publishEvent("driver.availability.changed", availabilityData);
        log.debug("Published driver availability for driver: {}", driverId);
    }
}
