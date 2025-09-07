package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for ride fare estimation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideEstimateRequest {
    private LocationDto pickupLocation;
    private LocationDto dropoffLocation;
    private String vehicleType; // Optional: ECONOMY, PREMIUM, SUV, etc.
    private Integer passengerCount; // Optional: number of passengers
    private Boolean sharedRide; // Optional: whether it's a shared ride
}
