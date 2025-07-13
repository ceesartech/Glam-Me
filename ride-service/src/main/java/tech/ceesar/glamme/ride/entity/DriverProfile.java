package tech.ceesar.glamme.ride.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {
    @Id
    @GeneratedValue
    private UUID driverId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private double currentLatitude;

    @Column(nullable = false)
    private double currentLongitude;

    @Column(nullable = false)
    private boolean available;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
