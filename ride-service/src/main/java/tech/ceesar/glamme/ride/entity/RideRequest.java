package tech.ceesar.glamme.ride.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RideRequest {
    @Id @GeneratedValue
    private UUID rideRequestId;

    private UUID customerId;

    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    private ProviderType providerType;

    private double pickupLatitude;

    private double pickupLongitude;

    private double dropoffLatitude;

    private double dropoffLongitude;

    @CreationTimestamp
    private Instant requestTime;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    private String externalRideId;

    private UUID driverId;

    private Instant cancelTime;

    private Instant completeTime;

    // **New: fare fields**
    private double estimatedFare;

    private double actualFare;

    private String currency;

    // Helper methods for compatibility
    public String getRideId() {
        return rideRequestId != null ? rideRequestId.toString() : null;
    }

    // Additional helper methods for analytics
    public void setActualDistanceMiles(double distance) {
        // Distance not stored in this entity - could be added if needed
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        // UpdatedAt not stored in this entity - could be added if needed
    }
}
