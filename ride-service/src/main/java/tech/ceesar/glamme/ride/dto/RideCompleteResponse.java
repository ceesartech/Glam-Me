package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RideCompleteResponse {
    private UUID rideId;
    private double actualFare;
    private String currency;
}
