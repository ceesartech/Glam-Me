package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Booking response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingId;
    private String customerId;
    private String stylistId;
    private String serviceId;
    private String serviceName;
    private String serviceDescription;
    private LocalDateTime appointmentDate;
    private Integer durationMinutes;
    private BigDecimal price;
    private Booking.Status status;
    private Booking.PaymentStatus paymentStatus;
    private String paymentIntentId;
    private String notes;
    private String specialRequests;
    private Set<String> addons;
    private Booking.LocationType locationType;
    private String locationAddress;
    private BigDecimal locationLatitude;
    private BigDecimal locationLongitude;
    private String calendarEventId;
    private String googleCalendarId;
    private String appleCalendarUrl;
    private String confirmationCode;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private Boolean reminderSent;
    private Boolean confirmationSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
