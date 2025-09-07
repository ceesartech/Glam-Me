package tech.ceesar.glamme.ride.controller;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.service.RideService;
import tech.ceesar.glamme.ride.service.aws.RideProviderService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Slf4j
public class RideController {

    private final RideService rideService;
    private final RideProviderService rideProviderService;

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

    /**
     * Get ride estimates from external providers
     */
    @PostMapping("/estimates")
    public ResponseEntity<List<RideEstimateResponse>> getRideEstimates(
            @RequestBody RideEstimateRequest request) {

        var uberEstimate = rideProviderService.getUberEstimate(
                request.pickupLocation(), request.dropoffLocation())
                .map(estimate -> new RideEstimateResponse(
                        "UBER", estimate.productId(), estimate.displayName(),
                        estimate.estimate(), estimate.distance(), estimate.duration()))
                .onErrorResume(error -> {
                    log.warn("Failed to get Uber estimate", error);
                    return reactor.core.publisher.Mono.empty();
                });

        var lyftEstimate = rideProviderService.getLyftEstimate(
                request.pickupLocation(), request.dropoffLocation())
                .map(estimate -> new RideEstimateResponse(
                        "LYFT", estimate.productId(), estimate.displayName(),
                        estimate.estimate(), estimate.distance(), estimate.duration()))
                .onErrorResume(error -> {
                    log.warn("Failed to get Lyft estimate", error);
                    return reactor.core.publisher.Mono.empty();
                });

        return reactor.core.publisher.Mono.zip(uberEstimate, lyftEstimate)
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()))
                .defaultIfEmpty(List.of())
                .map(ResponseEntity::ok)
                .block();
    }

    /**
     * Get ride estimate from specific provider
     */
    @PostMapping("/estimates/{provider}")
    public ResponseEntity<RideEstimateResponse> getRideEstimate(
            @PathVariable String provider,
            @RequestBody RideEstimateRequest request) {

        var estimate = switch (provider.toUpperCase()) {
            case "UBER" -> rideProviderService.getUberEstimate(
                    request.pickupLocation(), request.dropoffLocation());
            case "LYFT" -> rideProviderService.getLyftEstimate(
                    request.pickupLocation(), request.dropoffLocation());
            default -> reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Unsupported provider: " + provider));
        };

        return estimate
                .map(est -> new RideEstimateResponse(
                        provider.toUpperCase(), est.productId(), est.displayName(),
                        est.estimate(), est.distance(), est.duration()))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Failed to get {} estimate", provider, error);
                    return reactor.core.publisher.Mono.just(ResponseEntity.badRequest().build());
                })
                .block();
    }

    /**
     * Get ride details from external provider
     */
    @GetMapping("/{rideId}/details")
    public ResponseEntity<RideDetailsResponse> getRideDetails(
            @PathVariable UUID rideId) {

        var ride = rideService.getRideFromCache(rideId);
        if (ride == null || ride.getProviderType().toString().equals("INTERNAL")) {
            return ResponseEntity.badRequest().build();
        }

        var details = rideProviderService.getRideDetails(
                ride.getProviderType().toString(),
                ride.getExternalRideId())
                .map(det -> new RideDetailsResponse(
                        det.rideId(), det.status(), det.driverName(),
                        det.driverPhone(), det.vehicleModel(), det.vehicleLicense(),
                        det.eta()))
                .onErrorResume(error -> {
                    log.error("Failed to get ride details for {}", rideId, error);
                    return reactor.core.publisher.Mono.empty();
                });

        return details
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .block();
    }

    // Request/Response DTOs for new endpoints
    public record RideEstimateRequest(LocationDto pickupLocation, LocationDto dropoffLocation) {}
    public record RideEstimateResponse(String provider, String productId, String displayName,
                                     String estimate, Double distance, Integer duration) {}
    public record RideDetailsResponse(String rideId, String status, String driverName,
                                    String driverPhone, String vehicleModel, String vehicleLicense,
                                    Integer eta) {}
}
