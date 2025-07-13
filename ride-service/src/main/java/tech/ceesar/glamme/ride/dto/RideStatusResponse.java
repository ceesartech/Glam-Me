package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RideStatusResponse {
    private UUID rideId;
    private ProviderType providerType;
    private RideStatus status;
    private String externalRideId;         // for external
    private UUID driverId;                 // for internal
}
