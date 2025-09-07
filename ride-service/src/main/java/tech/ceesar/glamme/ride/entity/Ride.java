package tech.ceesar.glamme.ride.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ride_id", nullable = false, unique = true)
    private String rideId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "driver_id")
    private String driverId;
    
    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "pickup_latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal pickupLatitude;
    
    @Column(name = "pickup_longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal pickupLongitude;
    
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;
    
    @Column(name = "dropoff_latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal dropoffLatitude;
    
    @Column(name = "dropoff_longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal dropoffLongitude;
    
    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;
    
    @Column(name = "estimated_distance_miles", precision = 8, scale = 2)
    private BigDecimal estimatedDistanceMiles;
    
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    
    @Column(name = "estimated_fare", precision = 10, scale = 2)
    private BigDecimal estimatedFare;
    
    @Column(name = "actual_distance_miles", precision = 8, scale = 2)
    private BigDecimal actualDistanceMiles;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @Column(name = "actual_fare", precision = 10, scale = 2)
    private BigDecimal actualFare;
    
    @Column(name = "surge_multiplier", precision = 3, scale = 2)
    private BigDecimal surgeMultiplier;
    
    @Column(name = "payment_intent_id")
    private String paymentIntentId;
    
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    @Column(name = "external_ride_id")
    private String externalRideId;
    
    @Column(name = "external_driver_id")
    private String externalDriverId;
    
    @Column(name = "driver_name")
    private String driverName;
    
    @Column(name = "driver_phone")
    private String driverPhone;
    
    @Column(name = "vehicle_make")
    private String vehicleMake;
    
    @Column(name = "vehicle_model")
    private String vehicleModel;
    
    @Column(name = "vehicle_license_plate")
    private String vehicleLicensePlate;
    
    @Column(name = "vehicle_color")
    private String vehicleColor;
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "cancelled_by")
    private String cancelledBy;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Provider {
        UBER, LYFT, INTERNAL
    }
    
    public enum Status {
        REQUESTED, ACCEPTED, ARRIVED, STARTED, COMPLETED, CANCELLED, NO_SHOW
    }
    
    public enum PaymentStatus {
        PENDING, HELD, CAPTURED, REFUNDED, FAILED
    }
}