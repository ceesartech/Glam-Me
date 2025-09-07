package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelRideResponse {
    private boolean cancelled;
    private String message;

    // Constructor for backwards compatibility
    public CancelRideResponse(boolean cancelled) {
        this.cancelled = cancelled;
        this.message = cancelled ? "Ride cancelled successfully" : "Failed to cancel ride";
    }
}
