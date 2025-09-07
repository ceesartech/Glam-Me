package tech.ceesar.glamme.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.ride.controller.WebSocketController;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.entity.Ride;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repository.RideRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive notification service for ride status updates and alerts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;
    private final RideRepository rideRepository;
    private final WebSocketController webSocketController;

    /**
     * Send ride status update notification
     */
    public void sendRideStatusUpdate(String rideId, Ride.Status status, String message) {
        log.info("Sending ride status update - Ride: {}, Status: {}, Message: {}", rideId, status, message);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, status.toString(), message);

            // Send push notification to customer
            sendCustomerNotification(rideId, "RIDE_STATUS_UPDATE",
                    "Ride Status Update", message, Map.of("status", status.toString()));

            // Send push notification to driver if ride is assigned
            RideRequest ride = rideRepository.findByRideId(rideId).orElse(null);
            if (ride != null && ride.getDriverId() != null) {
                sendDriverNotification(ride.getDriverId().toString(), "RIDE_STATUS_UPDATE",
                        "Ride Status Update", message, Map.of("status", status.toString()));
            }

            // Publish event for external systems
            eventPublisher.publishEvent("glamme-bus",
                    Map.of(
                            "eventType", "RIDE_STATUS_UPDATE",
                            "rideId", rideId,
                            "status", status.toString(),
                            "message", message,
                            "timestamp", LocalDateTime.now().toString()
                    ));

        } catch (Exception e) {
            log.error("Failed to send ride status update notification: {}", e.getMessage());
        }
    }

    /**
     * Send driver assignment notification
     */
    public void sendDriverAssignmentNotification(String rideId, String driverId, String driverName, String vehicleInfo) {
        log.info("Sending driver assignment notification - Ride: {}, Driver: {}", rideId, driverId);

        String message = String.format("Your driver %s is on the way in a %s", driverName, vehicleInfo);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, "DRIVER_ASSIGNED", message);

            // Send push notification to customer
            sendCustomerNotification(rideId, "DRIVER_ASSIGNED",
                    "Driver Assigned", message, Map.of(
                            "driverId", driverId,
                            "driverName", driverName,
                            "vehicleInfo", vehicleInfo
                    ));

            // Send push notification to driver
            sendDriverNotification(driverId, "RIDE_ASSIGNED",
                    "New Ride Assigned", "You have been assigned a new ride", Map.of(
                            "rideId", rideId,
                            "customerId", getCustomerIdFromRide(rideId)
                    ));

        } catch (Exception e) {
            log.error("Failed to send driver assignment notification: {}", e.getMessage());
        }
    }

    /**
     * Send ETA update notification
     */
    public void sendETAUpdateNotification(String rideId, Integer etaMinutes) {
        log.info("Sending ETA update notification - Ride: {}, ETA: {} minutes", rideId, etaMinutes);

        String message = String.format("Your driver will arrive in approximately %d minutes", etaMinutes);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, "ETA_UPDATE", message);

            // Send push notification to customer
            sendCustomerNotification(rideId, "ETA_UPDATE",
                    "ETA Update", message, Map.of("etaMinutes", etaMinutes.toString()));

        } catch (Exception e) {
            log.error("Failed to send ETA update notification: {}", e.getMessage());
        }
    }

    /**
     * Send ride completion notification
     */
    public void sendRideCompletionNotification(String rideId, BigDecimal fare, String currency) {
        log.info("Sending ride completion notification - Ride: {}, Fare: {} {}", rideId, fare, currency);

        String message = String.format("Your ride is complete. Total fare: %s %s", fare.toString(), currency);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, "RIDE_COMPLETED", message);

            // Send push notification to customer
            sendCustomerNotification(rideId, "RIDE_COMPLETED",
                    "Ride Completed", message, Map.of(
                            "fare", fare.toString(),
                            "currency", currency
                    ));

            // Send push notification to driver
            RideRequest ride = rideRepository.findByRideId(rideId).orElse(null);
            if (ride != null && ride.getDriverId() != null) {
                String driverMessage = String.format("Ride completed. Earnings: %s %s",
                        fare.multiply(BigDecimal.valueOf(0.8)).toString(), currency); // 80% to driver
                sendDriverNotification(ride.getDriverId().toString(), "RIDE_COMPLETED",
                        "Ride Completed", driverMessage, Map.of(
                                "fare", fare.toString(),
                                "earnings", fare.multiply(BigDecimal.valueOf(0.8)).toString(),
                                "currency", currency
                        ));
            }

        } catch (Exception e) {
            log.error("Failed to send ride completion notification: {}", e.getMessage());
        }
    }

    /**
     * Send ride cancellation notification
     */
    public void sendRideCancellationNotification(String rideId, String reason, String cancelledBy) {
        log.info("Sending ride cancellation notification - Ride: {}, Reason: {}, Cancelled by: {}",
                rideId, reason, cancelledBy);

        String message = String.format("Your ride has been cancelled. Reason: %s", reason);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, "RIDE_CANCELLED", message);

            // Send push notification to both customer and driver
            sendCustomerNotification(rideId, "RIDE_CANCELLED",
                    "Ride Cancelled", message, Map.of(
                            "reason", reason,
                            "cancelledBy", cancelledBy
                    ));

            RideRequest ride = rideRepository.findByRideId(rideId).orElse(null);
            if (ride != null && ride.getDriverId() != null) {
                sendDriverNotification(ride.getDriverId().toString(), "RIDE_CANCELLED",
                        "Ride Cancelled", "The ride has been cancelled by the customer", Map.of(
                                "reason", reason,
                                "cancelledBy", cancelledBy
                        ));
            }

        } catch (Exception e) {
            log.error("Failed to send ride cancellation notification: {}", e.getMessage());
        }
    }

    /**
     * Send payment confirmation notification
     */
    public void sendPaymentConfirmationNotification(String rideId, BigDecimal amount, String currency) {
        log.info("Sending payment confirmation notification - Ride: {}, Amount: {} {}",
                rideId, amount, currency);

        String message = String.format("Payment of %s %s has been processed successfully", amount, currency);

        try {
            // Send push notification to customer
            sendCustomerNotification(rideId, "PAYMENT_CONFIRMED",
                    "Payment Confirmed", message, Map.of(
                            "amount", amount.toString(),
                            "currency", currency
                    ));

        } catch (Exception e) {
            log.error("Failed to send payment confirmation notification: {}", e.getMessage());
        }
    }

    /**
     * Send driver arrival notification
     */
    public void sendDriverArrivalNotification(String rideId, String driverName, String vehicleInfo) {
        log.info("Sending driver arrival notification - Ride: {}, Driver: {}", rideId, driverName);

        String message = String.format("Your driver %s has arrived in a %s", driverName, vehicleInfo);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, "DRIVER_ARRIVED", message);

            // Send push notification to customer
            sendCustomerNotification(rideId, "DRIVER_ARRIVED",
                    "Driver Arrived", message, Map.of(
                            "driverName", driverName,
                            "vehicleInfo", vehicleInfo
                    ));

        } catch (Exception e) {
            log.error("Failed to send driver arrival notification: {}", e.getMessage());
        }
    }

    /**
     * Send promotional notifications
     */
    public void sendPromotionalNotification(String customerId, String title, String message, Map<String, String> metadata) {
        log.info("Sending promotional notification to customer: {}", customerId);

        try {
            // Send push notification to customer
            sendCustomerNotification(customerId, "PROMOTION",
                    title, message, metadata != null ? metadata : Map.of());

        } catch (Exception e) {
            log.error("Failed to send promotional notification: {}", e.getMessage());
        }
    }

    /**
     * Send safety alert notification
     */
    public void sendSafetyAlertNotification(String rideId, String alertType, String message) {
        log.info("Sending safety alert notification - Ride: {}, Type: {}", rideId, alertType);

        try {
            // Send WebSocket notification
            webSocketController.broadcastRideStatusUpdate(rideId, "SAFETY_ALERT", message);

            // Send push notification to customer
            sendCustomerNotification(rideId, "SAFETY_ALERT",
                    "Safety Alert", message, Map.of("alertType", alertType));

            // Send push notification to driver
            RideRequest ride = rideRepository.findByRideId(rideId).orElse(null);
            if (ride != null && ride.getDriverId() != null) {
                sendDriverNotification(ride.getDriverId().toString(), "SAFETY_ALERT",
                        "Safety Alert", message, Map.of("alertType", alertType));
            }

            // Broadcast to emergency monitoring systems
            messagingTemplate.convertAndSend("/topic/emergency/alerts",
                    new EmergencyAlert(rideId, alertType, message, null, LocalDateTime.now()));

        } catch (Exception e) {
            log.error("Failed to send safety alert notification: {}", e.getMessage());
        }
    }

    /**
     * Send surge pricing notification
     */
    public void sendSurgePricingNotification(String rideId, BigDecimal surgeMultiplier) {
        log.info("Sending surge pricing notification - Ride: {}, Surge: {}", rideId, surgeMultiplier);

        String message = String.format("Surge pricing is active. Current multiplier: %sx",
                surgeMultiplier.toString());

        try {
            // Send push notification to customer
            sendCustomerNotification(rideId, "SURGE_PRICING",
                    "Surge Pricing Active", message, Map.of(
                            "surgeMultiplier", surgeMultiplier.toString()
                    ));

        } catch (Exception e) {
            log.error("Failed to send surge pricing notification: {}", e.getMessage());
        }
    }

    /**
     * Send customer notification via WebSocket
     */
    private void sendCustomerNotification(String rideId, String type, String title, String message, Map<String, String> metadata) {
        try {
            RideRequest ride = rideRepository.findByRideId(rideId).orElse(null);
            if (ride != null) {
                NotificationDto notification = NotificationDto.builder()
                        .rideId(rideId)
                        .customerId(ride.getCustomerId().toString())
                        .type(type)
                        .title(title)
                        .message(message)
                        .metadata(metadata)
                        .timestamp(LocalDateTime.now())
                        .build();

                // Send via WebSocket to customer
                messagingTemplate.convertAndSendToUser(
                        ride.getCustomerId().toString(),
                        "/queue/notifications",
                        notification
                );
            }
        } catch (Exception e) {
            log.error("Failed to send customer notification: {}", e.getMessage());
        }
    }

    /**
     * Send driver notification via WebSocket
     */
    private void sendDriverNotification(String driverId, String type, String title, String message, Map<String, String> metadata) {
        try {
            NotificationDto notification = NotificationDto.builder()
                    .driverId(driverId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .metadata(metadata)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Send via WebSocket to driver
            messagingTemplate.convertAndSendToUser(
                    driverId,
                    "/queue/notifications",
                    notification
            );
        } catch (Exception e) {
            log.error("Failed to send driver notification: {}", e.getMessage());
        }
    }

    /**
     * Get customer ID from ride
     */
    private String getCustomerIdFromRide(String rideId) {
        try {
            RideRequest ride = rideRepository.findByRideId(rideId).orElse(null);
            return ride != null ? ride.getCustomerId().toString() : null;
        } catch (Exception e) {
            log.error("Failed to get customer ID from ride: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Scheduled task to send reminder notifications
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void sendRideReminders() {
        log.info("Checking for rides that need reminders");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesFromNow = now.plusMinutes(5);

        try {
            // Find rides starting in the next 5 minutes
            List<RideRequest> upcomingRides = rideRepository.findRidesByStatusAndDateRange(
                    RideStatus.ACCEPTED.toString(), now, fiveMinutesFromNow);

            for (RideRequest ride : upcomingRides) {
                if (ride.getRequestTime().isAfter(java.time.Instant.from(now.atZone(java.time.ZoneId.systemDefault()))) && ride.getRequestTime().isBefore(java.time.Instant.from(fiveMinutesFromNow.atZone(java.time.ZoneId.systemDefault())))) {
                    sendRideReminderNotification(ride.getRideId(), 5);
                }
            }

        } catch (Exception e) {
            log.error("Failed to send ride reminders: {}", e.getMessage());
        }
    }

    /**
     * Send ride reminder notification
     */
    private void sendRideReminderNotification(String rideId, int minutesUntilPickup) {
        log.info("Sending ride reminder - Ride: {}, Minutes until pickup: {}", rideId, minutesUntilPickup);

        String message = String.format("Your ride is scheduled to start in %d minutes", minutesUntilPickup);

        try {
            sendCustomerNotification(rideId, "RIDE_REMINDER",
                    "Ride Reminder", message, Map.of("minutesUntilPickup", String.valueOf(minutesUntilPickup)));

        } catch (Exception e) {
            log.error("Failed to send ride reminder notification: {}", e.getMessage());
        }
    }

    // Inner DTO classes for notifications

    public static class NotificationDto {
        private String rideId;
        private String customerId;
        private String driverId;
        private String type;
        private String title;
        private String message;
        private Map<String, String> metadata;
        private LocalDateTime timestamp;

        public static NotificationDtoBuilder builder() {
            return new NotificationDtoBuilder();
        }

        public static class NotificationDtoBuilder {
            private String rideId;
            private String customerId;
            private String driverId;
            private String type;
            private String title;
            private String message;
            private Map<String, String> metadata;
            private LocalDateTime timestamp;

            public NotificationDtoBuilder rideId(String rideId) {
                this.rideId = rideId;
                return this;
            }

            public NotificationDtoBuilder customerId(String customerId) {
                this.customerId = customerId;
                return this;
            }

            public NotificationDtoBuilder driverId(String driverId) {
                this.driverId = driverId;
                return this;
            }

            public NotificationDtoBuilder type(String type) {
                this.type = type;
                return this;
            }

            public NotificationDtoBuilder title(String title) {
                this.title = title;
                return this;
            }

            public NotificationDtoBuilder message(String message) {
                this.message = message;
                return this;
            }

            public NotificationDtoBuilder metadata(Map<String, String> metadata) {
                this.metadata = metadata;
                return this;
            }

            public NotificationDtoBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public NotificationDto build() {
                NotificationDto dto = new NotificationDto();
                dto.rideId = this.rideId;
                dto.customerId = this.customerId;
                dto.driverId = this.driverId;
                dto.type = this.type;
                dto.title = this.title;
                dto.message = this.message;
                dto.metadata = this.metadata;
                dto.timestamp = this.timestamp;
                return dto;
            }
        }

        // Getters and setters
        public String getRideId() { return rideId; }
        public void setRideId(String rideId) { this.rideId = rideId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getDriverId() { return driverId; }
        public void setDriverId(String driverId) { this.driverId = driverId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class EmergencyAlert {
        private String rideId;
        private String type;
        private String message;
        private LocationDto location;
        private LocalDateTime timestamp;

        public EmergencyAlert(String rideId, String type, String message, LocationDto location, LocalDateTime timestamp) {
            this.rideId = rideId;
            this.type = type;
            this.message = message;
            this.location = location;
            this.timestamp = timestamp;
        }

        // Getters
        public String getRideId() { return rideId; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public LocationDto getLocation() { return location; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
