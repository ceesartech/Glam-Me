package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancelRideResponse {
    private boolean cancelled;
}
