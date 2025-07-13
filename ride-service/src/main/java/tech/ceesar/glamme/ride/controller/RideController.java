package tech.ceesar.glamme.ride.controller;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.service.RideService;

import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping
    public ResponseEntity<CreateRideResponse> requestRide(
            @RequestBody CreateRideRequest req
    ) {
        CreateRideResponse resp = rideService.createRide(req);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/{rideId}/status")
    public RideStatusResponse getStatus(
            @PathVariable UUID rideId
    ) {
        return rideService.getStatus(rideId);
    }

    @PostMapping("/{rideId}/cancel")
    public CancelRideResponse cancelRide(
            @PathVariable UUID rideId
    ) {
        return rideService.cancelRide(rideId);
    }

    @PostMapping("/{rideId}/complete")
    public RideCompleteResponse completeRide(@PathVariable UUID rideId) throws StripeException {
        return rideService.completeRide(rideId);
    }
}
