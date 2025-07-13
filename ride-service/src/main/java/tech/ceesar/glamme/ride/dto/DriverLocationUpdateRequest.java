package tech.ceesar.glamme.ride.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class DriverLocationUpdateRequest {
    private UUID driverId;
    private double latitude;
    private double longitude;
}
