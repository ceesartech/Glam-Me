package tech.ceesar.glamme.ride.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ride_tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ride_id", nullable = false)
    private String rideId;
    
    @Column(name = "driver_id", nullable = false)
    private String driverId;
    
    @Column(name = "latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal longitude;
    
    @Column(name = "heading")
    private Integer heading;
    
    @Column(name = "speed_mph")
    private BigDecimal speedMph;
    
    @Column(name = "accuracy_meters")
    private BigDecimal accuracyMeters;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        ACTIVE,      // Currently tracking
        PAUSED,      // Temporarily paused
        COMPLETED,   // Ride completed
        CANCELLED    // Ride cancelled
    }
}