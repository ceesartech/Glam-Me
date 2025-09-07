package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ride notifications and alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideNotificationDto {
    private String notificationId;
    private String rideId;
    private String customerId;
    private String driverId;
    private String notificationType; // RIDE_REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVED, etc.
    private String title;
    private String message;
    private String channel; // PUSH, SMS, EMAIL, WEBSOCKET
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private Boolean read;
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private String actionUrl; // URL for user to take action
    private String metadata; // Additional JSON metadata

    // Predefined notification types
    public enum NotificationType {
        RIDE_REQUESTED,
        DRIVER_ASSIGNED,
        DRIVER_ARRIVED,
        RIDE_STARTED,
        RIDE_COMPLETED,
        RIDE_CANCELLED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        DRIVER_DELAYED,
        ETA_UPDATED,
        SURGE_PRICING,
        SAFETY_ALERT,
        EMERGENCY_CONTACT
    }
}
