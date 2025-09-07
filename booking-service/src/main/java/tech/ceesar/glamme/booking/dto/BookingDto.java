package tech.ceesar.glamme.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingDto {
    
    private Long id;
    
    private String bookingId;
    
    private String customerId;
    
    private String stylistId;
    
    private Booking.ServiceType serviceType;
    
    private Booking.Status status;
    
    private LocalDateTime scheduledDate;
    
    private Integer durationMinutes;
    
    private Booking.LocationType locationType;
    
    private String address;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private BigDecimal totalAmount;
    
    private BigDecimal serviceFee;
    
    private BigDecimal taxAmount;
    
    private BigDecimal tipAmount;
    
    private Booking.PaymentStatus paymentStatus;
    
    private String paymentIntentId;
    
    private String stripeCustomerId;
    
    private String specialRequests;
    
    private String notes;
    
    private String cancellationReason;
    
    private LocalDateTime cancelledAt;
    
    private String cancelledBy;
    
    private BigDecimal refundAmount;
    
    private String refundReason;
    
    private LocalDateTime refundedAt;
    
    private LocalDateTime completedAt;
    
    private Integer rating;
    
    private String review;
    
    private Set<Booking.Service> services;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Additional fields for display
    private String customerName;
    private String stylistName;
    private String stylistPhone;
    private String stylistEmail;
}
