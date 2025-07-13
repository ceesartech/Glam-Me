package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateRideResponse {
    private UUID rideId;
    private String externalRideId;         // null for internal
}
