package tech.ceesar.glamme.ride.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.ride.entity.Ride;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideDto {
    
    private Long id;
    
    private String rideId;
    
    private String customerId;
    
    private String bookingId;
    
    private Ride.Provider provider;
    
    private Ride.Status status;
    
    private String pickupAddress;
    
    private BigDecimal pickupLatitude;
    
    private BigDecimal pickupLongitude;
    
    private String dropoffAddress;
    
    private BigDecimal dropoffLatitude;
    
    private BigDecimal dropoffLongitude;
    
    private BigDecimal estimatedDistanceMiles;
    
    private Integer estimatedDurationMinutes;
    
    private BigDecimal actualDistanceMiles;
    
    private Integer actualDurationMinutes;
    
    private BigDecimal estimatedFare;
    
    private BigDecimal actualFare;
    
    private BigDecimal serviceFee;
    
    private BigDecimal totalAmount;
    
    private Ride.PaymentStatus paymentStatus;
    
    private String paymentIntentId;
    
    private String stripeCustomerId;
    
    private String externalRideId;
    
    private String driverName;
    
    private String driverPhone;
    
    private String vehicleMake;
    
    private String vehicleModel;
    
    private String vehicleLicensePlate;
    
    private LocalDateTime requestedAt;
    
    private LocalDateTime acceptedAt;
    
    private LocalDateTime arrivedAt;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private LocalDateTime cancelledAt;
    
    private String cancelledBy;
    
    private String cancellationReason;
    
    private BigDecimal refundAmount;
    
    private String refundReason;
    
    private LocalDateTime refundedAt;
    
    private Integer rating;
    
    private String review;
    
    private String specialRequests;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Additional fields for display
    private String customerName;
    private String customerPhone;
    private RideTrackingDto latestTracking;
}
