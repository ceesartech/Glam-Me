package tech.ceesar.glamme.ride.client;

import tech.ceesar.glamme.ride.dto.CreateRideRequest;
import tech.ceesar.glamme.ride.dto.RideStatusResponse;

public interface ExternalRideClient {
    /**
     * Sends a ride request to external provider.
     * Returns the external ride ID.
     */
    String requestRide(CreateRideRequest req);

    /**
     * Fetches the current status for an external ride.
     */
    RideStatusResponse getStatus(String externalRideId);

    /**
     * Cancels the external ride; returns true if successful.
     */
    boolean cancelRide(String externalRideId);
}
