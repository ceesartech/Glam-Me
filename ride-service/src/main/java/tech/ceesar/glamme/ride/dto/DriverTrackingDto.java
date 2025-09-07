package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for driver location and status tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTrackingDto {
    private String driverId;
    private String rideId; // Current ride if assigned
    private LocationDto currentLocation;
    private Integer heading; // Direction in degrees (0-360)
    private BigDecimal speedMph; // Current speed
    private BigDecimal accuracyMeters; // GPS accuracy
    private String status; // AVAILABLE, BUSY, OFFLINE, ON_RIDE
    private LocalDateTime lastUpdated;
    private LocalDateTime lastRideCompleted;
    private Integer ridesCompletedToday;
    private BigDecimal earningsToday;
    private BigDecimal rating; // Driver rating 1-5
    private Boolean online; // Whether driver is online
    private String vehicleId;
    private String vehicleType;
    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;
    private BigDecimal distanceTraveledToday;
    private Integer hoursOnlineToday;
}
