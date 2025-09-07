package tech.ceesar.glamme.ride.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import tech.ceesar.glamme.ride.dto.DriverTrackingDto;
import tech.ceesar.glamme.ride.dto.LocationDto;
import tech.ceesar.glamme.ride.dto.RideTrackingDto;
import tech.ceesar.glamme.ride.service.DriverTrackingService;
import tech.ceesar.glamme.ride.service.RideTrackingService;

import java.time.LocalDateTime;

/**
 * WebSocket controller for real-time ride and driver communication
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RideTrackingService rideTrackingService;
    private final DriverTrackingService driverTrackingService;

    /**
     * Handle driver location updates via WebSocket
     */
    @MessageMapping("/driver/{driverId}/location")
    @SendTo("/topic/driver/{driverId}/location")
    public DriverTrackingDto handleDriverLocationUpdate(
            @DestinationVariable String driverId,
            @Payload LocationDto location) {

        log.info("Received WebSocket location update for driver: {}", driverId);

        try {
            // Update driver location
            rideTrackingService.updateDriverLocation(
                    driverId,
                    location,
                    null, // heading
                    null, // speed
                    null  // accuracy
            );

            // Get updated driver status
            return driverTrackingService.getDriverStatus(driverId)
                    .orElse(new DriverTrackingDto());

        } catch (Exception e) {
            log.error("Error processing driver location update: {}", e.getMessage());
            return new DriverTrackingDto();
        }
    }

    /**
     * Handle ride tracking updates via WebSocket
     */
    @MessageMapping("/ride/{rideId}/tracking")
    @SendTo("/topic/ride/{rideId}/progress")
    public RideTrackingDto handleRideTrackingUpdate(
            @DestinationVariable String rideId,
            @Payload RideTrackingDto trackingData) {

        log.info("Received WebSocket ride tracking update for ride: {}", rideId);

        try {
            // Update ride progress
            rideTrackingService.updateRideProgress(
                    rideId,
                    trackingData.getDriverId(),
                    trackingData.getCurrentLocation(),
                    trackingData.getDistanceTraveled(),
                    trackingData.getEtaToDestination()
            );

            // Return updated tracking data
            return rideTrackingService.getLatestRideTracking(rideId)
                    .orElse(trackingData);

        } catch (Exception e) {
            log.error("Error processing ride tracking update: {}", e.getMessage());
            return trackingData;
        }
    }

    /**
     * Handle driver status updates via WebSocket
     */
    @MessageMapping("/driver/{driverId}/status")
    @SendTo("/topic/driver/{driverId}/status")
    public DriverTrackingDto handleDriverStatusUpdate(
            @DestinationVariable String driverId,
            @Payload String status) {

        log.info("Received WebSocket status update for driver: {} - Status: {}", driverId, status);

        try {
            // Update driver status
            driverTrackingService.updateDriverStatus(
                    driverId,
                    status,
                    status.equals("AVAILABLE"), // available based on status
                    null // no location update
            );

            // Get updated driver status
            return driverTrackingService.getDriverStatus(driverId)
                    .orElse(new DriverTrackingDto());

        } catch (Exception e) {
            log.error("Error processing driver status update: {}", e.getMessage());
            return new DriverTrackingDto();
        }
    }

    /**
     * Handle customer ride requests via WebSocket
     */
    @MessageMapping("/customer/{customerId}/ride")
    @SendTo("/topic/customer/{customerId}/ride")
    public String handleCustomerRideUpdate(
            @DestinationVariable String customerId,
            @Payload String rideUpdate) {

        log.info("Received WebSocket ride update for customer: {}", customerId);

        // Echo back the update for now - in production, you'd process and validate
        return rideUpdate;
    }

    /**
     * Handle emergency/SOS alerts via WebSocket
     */
    @MessageMapping("/emergency/{rideId}")
    @SendTo("/topic/emergency/alerts")
    public EmergencyAlert handleEmergencyAlert(
            @DestinationVariable String rideId,
            @Payload EmergencyAlert alert) {

        log.warn("EMERGENCY ALERT received for ride: {} - Type: {}", rideId, alert.getType());

        // Broadcast emergency alert to all monitoring systems
        messagingTemplate.convertAndSend("/topic/emergency/alerts", alert);
        messagingTemplate.convertAndSend("/topic/admin/alerts", alert);

        return alert;
    }

    /**
     * Handle chat messages between driver and customer
     */
    @MessageMapping("/chat/{rideId}")
    @SendTo("/topic/chat/{rideId}")
    public ChatMessage handleChatMessage(
            @DestinationVariable String rideId,
            @Payload ChatMessage message) {

        log.info("Chat message in ride {} from {}: {}",
                rideId, message.getSender(), message.getContent());

        // Add timestamp
        message.setTimestamp(LocalDateTime.now());

        return message;
    }

    /**
     * Broadcast ride status updates to subscribers
     */
    public void broadcastRideStatusUpdate(String rideId, String status, String message) {
        RideStatusUpdate update = new RideStatusUpdate(rideId, status, message, LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/status", update);
        messagingTemplate.convertAndSend("/topic/rides/status", update);

        log.info("Broadcasted ride status update: {} - {}", rideId, status);
    }

    /**
     * Broadcast driver availability updates
     */
    public void broadcastDriverAvailability(String driverId, boolean available, LocationDto location) {
        DriverAvailabilityUpdate update = new DriverAvailabilityUpdate(
                driverId, available, location, LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/availability", update);
        messagingTemplate.convertAndSend("/topic/drivers/availability", update);

        log.info("Broadcasted driver availability update: {} - {}", driverId, available);
    }

    /**
     * Send private message to specific user
     */
    public void sendPrivateMessage(String userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
        log.info("Sent private message to user {} at {}", userId, destination);
    }

    // Inner classes for WebSocket message payloads

    public static class EmergencyAlert {
        private String rideId;
        private String type; // SOS, MEDICAL, ACCIDENT, etc.
        private String message;
        private LocationDto location;
        private LocalDateTime timestamp;

        // Getters and setters
        public String getRideId() { return rideId; }
        public void setRideId(String rideId) { this.rideId = rideId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LocationDto getLocation() { return location; }
        public void setLocation(LocationDto location) { this.location = location; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ChatMessage {
        private String sender;
        private String senderType; // DRIVER, CUSTOMER
        private String content;
        private LocalDateTime timestamp;

        // Getters and setters
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }

        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class RideStatusUpdate {
        private String rideId;
        private String status;
        private String message;
        private LocalDateTime timestamp;

        public RideStatusUpdate(String rideId, String status, String message, LocalDateTime timestamp) {
            this.rideId = rideId;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getRideId() { return rideId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class DriverAvailabilityUpdate {
        private String driverId;
        private boolean available;
        private LocationDto location;
        private LocalDateTime timestamp;

        public DriverAvailabilityUpdate(String driverId, boolean available, LocationDto location, LocalDateTime timestamp) {
            this.driverId = driverId;
            this.available = available;
            this.location = location;
            this.timestamp = timestamp;
        }

        // Getters
        public String getDriverId() { return driverId; }
        public boolean isAvailable() { return available; }
        public LocationDto getLocation() { return location; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
