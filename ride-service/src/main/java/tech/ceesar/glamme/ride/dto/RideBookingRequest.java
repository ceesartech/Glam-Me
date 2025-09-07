package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ride booking request to external providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideBookingRequest {
    private String customerId;
    private LocationDto pickupLocation;
    private LocationDto dropoffLocation;
    private String productId;
    private String surgeConfirmationId;


}
