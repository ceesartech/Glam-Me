package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating ride information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideUpdateRequest {
    private String status; // New status to update
    private LocationDto pickupLocation; // Updated pickup location
    private LocationDto dropoffLocation; // Updated dropoff location
    private BigDecimal estimatedFare; // Updated fare estimate
    private BigDecimal actualFare; // Actual fare charged
    private BigDecimal distanceTraveled; // Distance traveled
    private Integer durationMinutes; // Ride duration
    private String driverId; // Assign driver to ride
    private String vehicleId; // Assign vehicle to ride
    private String notes; // Additional notes
    private String cancellationReason; // Reason for cancellation
    private String specialInstructions; // Special pickup/dropoff instructions
}
