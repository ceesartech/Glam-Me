package tech.ceesar.glamme.ride.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String driverName;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private BigDecimal currentLatitude;

    @Column(nullable = false)
    private BigDecimal currentLongitude;

    @Column(nullable = false)
    private Boolean available;

    @Column
    private Boolean online;

    @Column
    private BigDecimal rating;

    @Column
    private String vehicleModel;

    @Column
    private String vehicleLicense;

    @Column
    private String vehicleId;

    @Column
    private LocalDateTime lastLocationUpdate;

    @Column
    private LocalDateTime lastStatusUpdate;

    @Column
    private LocalDateTime lastRideCompleted;

    @Column
    private LocalDateTime shiftStartTime;

    @Column
    private LocalDateTime shiftEndTime;

    @Column
    private Integer ridesCompletedToday;

    @Column
    private BigDecimal earningsToday;

    @Column
    private BigDecimal distanceTraveledToday;

    @Column
    private Integer hoursOnlineToday;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods for compatibility
    public String getName() {
        return driverName;
    }

    public void setName(String name) {
        this.driverName = name;
    }

    public double getCurrentLatitude() {
        return currentLatitude != null ? currentLatitude.doubleValue() : 0.0;
    }

    public void setCurrentLatitude(double currentLatitude) {
        this.currentLatitude = BigDecimal.valueOf(currentLatitude);
    }

    public double getCurrentLongitude() {
        return currentLongitude != null ? currentLongitude.doubleValue() : 0.0;
    }

    public void setCurrentLongitude(double currentLongitude) {
        this.currentLongitude = BigDecimal.valueOf(currentLongitude);
    }

    public boolean getAvailable() {
        return available != null ? available : false;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean getOnline() {
        return online != null ? online : false;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
