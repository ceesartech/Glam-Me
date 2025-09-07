package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for detailed ride information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideDetailsResponse {
    private String rideId; // Internal ride ID
    private String externalRideId; // Provider's ride ID
    private String status; // Current ride status
    private String provider; // UBER, LYFT, INTERNAL

    // Driver Information
    private String driverId; // Internal driver ID
    private String externalDriverId; // Provider's driver ID
    private String driverName;
    private String driverPhone;
    private String driverPhotoUrl;
    private BigDecimal driverRating; // Driver's rating (1-5)

    // Vehicle Information
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleColor;
    private String vehicleLicensePlate;
    private Integer vehicleYear;

    // Location Information
    private LocationDto currentLocation; // Driver's current location
    private Integer eta; // Estimated time of arrival in minutes
    private Integer etaToPickup; // ETA to pickup location

    // Ride Information
    private BigDecimal estimatedFare;
    private BigDecimal actualFare;
    private BigDecimal distanceTraveled;
    private Integer durationMinutes;
    private String currency;

    // Status Timestamps
    private String requestedAt;
    private String acceptedAt;
    private String arrivedAt;
    private String startedAt;
    private String completedAt;
    private String cancelledAt;
}
