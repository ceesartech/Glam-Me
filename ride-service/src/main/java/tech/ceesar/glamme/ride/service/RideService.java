package tech.ceesar.glamme.ride.service;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.common.service.RedisIdempotencyService;
import tech.ceesar.glamme.ride.client.ExternalRideClient;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repositories.RideRequestRepository;
import tech.ceesar.glamme.ride.service.aws.RideEventService;
import tech.ceesar.glamme.ride.service.aws.RideProviderService;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {
    @Autowired
    private PaymentService paymentService;
    private final RideRequestRepository rideRepo;
    private final DriverProfileRepository driverRepo;
    private final Map<ProviderType, ExternalRideClient> externalClients;
    private final RideProviderService rideProviderService;
    private final RideEventService rideEventService;
    private final RedisCacheService cacheService;
    private final RedisIdempotencyService idempotencyService;
    private final EventPublisher eventPublisher;

    /**
     * Create a new ride, either internal or external.
     */
    @Transactional
    public CreateRideResponse createRide(CreateRideRequest req) {
        // Idempotency check
        String idempotencyKey = generateRideIdempotencyKey(req);
        if (!idempotencyService.startRideOperation(req.getCustomerId(), idempotencyKey, req)) {
            throw new BadRequestException("Ride creation already in progress");
        }

        try {
            RideRequest ride = new RideRequest();
            ride.setCustomerId(req.getCustomerId());
            ride.setBookingId(req.getBookingId());
            ride.setProviderType(req.getProviderType());
            ride.setPickupLatitude(req.getPickupLocation().getLatitude());
            ride.setPickupLongitude(req.getPickupLocation().getLongitude());
            ride.setDropoffLatitude(req.getDropoffLocation().getLatitude());
            ride.setDropoffLongitude(req.getDropoffLocation().getLongitude());
            ride.setStatus(RideStatus.REQUESTED);

            if (req.getProviderType() == ProviderType.INTERNAL) {
                // dispatch internal
                DriverProfile driver = dispatchInternalRide(ride);
                ride.setDriverId(driver.getDriverId());
                ride.setStatus(RideStatus.ACCEPTED);

                // Publish internal ride accepted event
                rideEventService.publishRideAccepted(
                        ride.getRideRequestId().toString(),
                        driver.getDriverId().toString(),
                        driver.getDriverName(),
                        driver.getVehicleModel(),
                        driver.getVehicleLicense(),
                        5 // 5 minute ETA for internal rides
                );

            } else {
                // external provider (Uber/Lyft)
                var bookingRequest = new RideProviderService.RideBookingRequest(
                        req.getCustomerId(),
                        req.getPickupLocation(),
                        req.getDropoffLocation(),
                        req.getProductId(),
                        null // surge confirmation ID
                );

                var rideRequest = switch (req.getProviderType()) {
                    case UBER -> rideProviderService.requestUberRide(bookingRequest).block();
                    case LYFT -> rideProviderService.requestLyftRide(bookingRequest).block();
                    default -> throw new BadRequestException("Unsupported provider: " + req.getProviderType());
                };

                ride.setExternalRideId(rideRequest.rideId());
                ride.setStatus(RideStatus.REQUESTED);

                // Publish external ride requested event
                rideEventService.publishRideRequested(
                        rideRequest.rideId(),
                        req.getCustomerId(),
                        req.getProviderType().toString(),
                        req.getPickupLocation().getLatitude(),
                        req.getPickupLocation().getLongitude(),
                        req.getDropoffLocation().getLatitude(),
                        req.getDropoffLocation().getLongitude()
                );
            }

            ride = rideRepo.save(ride);

            // Cache the ride
            cacheService.set("ride:" + ride.getRideRequestId(), ride, Duration.ofHours(24));

            // Mark idempotency as completed
            idempotencyService.completeOperation(idempotencyKey, ride.getRideRequestId());

            log.info("Created ride {} for customer {} with provider {}",
                    ride.getRideRequestId(), req.getCustomerId(), req.getProviderType());

            return new CreateRideResponse(ride.getRideRequestId(), ride.getExternalRideId());

        } catch (Exception e) {
            // Mark idempotency as failed
            idempotencyService.failOperation(idempotencyKey, e.getMessage());
            log.error("Failed to create ride for customer {}", req.getCustomerId(), e);
            throw new RuntimeException("Failed to create ride", e);
        }
    }

    private String generateRideIdempotencyKey(CreateRideRequest req) {
        return "ride:" + req.getCustomerId() + ":" +
               req.getPickupLocation().getLatitude() + ":" +
               req.getPickupLocation().getLongitude() + ":" +
               req.getDropoffLocation().getLatitude() + ":" +
               req.getDropoffLocation().getLongitude() + ":" +
               Instant.now().toEpochMilli();
    }

    /**
     * Get current ride status.
     */
    @Transactional(readOnly = true)
    public RideStatusResponse getStatus(UUID rideId) {
        // Try cache first
        RideRequest ride = cacheService.get("ride:" + rideId, RideRequest.class)
                .orElseGet(() -> {
                    RideRequest dbRide = rideRepo.findById(rideId)
                            .orElseThrow(() -> new ResourceNotFoundException("Ride","id",rideId));
                    // Cache for 30 minutes
                    cacheService.set("ride:" + rideId, dbRide, Duration.ofMinutes(30));
                    return dbRide;
                });

        if (ride.getProviderType() == ProviderType.INTERNAL) {
            return new RideStatusResponse(
                    ride.getRideRequestId(),
                    ride.getProviderType(),
                    ride.getStatus(),
                    null,
                    ride.getDriverId()
            );
        } else {
            // Get real-time status from external provider
            var details = rideProviderService.getRideDetails(
                    ride.getProviderType().toString(),
                    ride.getExternalRideId()
            ).block();

            if (details != null) {
                // Update local status based on provider status
                updateRideStatusFromProvider(ride, details.status());

                return new RideStatusResponse(
                        ride.getRideRequestId(),
                        ride.getProviderType(),
                        ride.getStatus(),
                        details.driverName(),
                        ride.getDriverId()
                );
            } else {
                // Fallback to cached status
                return new RideStatusResponse(
                        ride.getRideRequestId(),
                        ride.getProviderType(),
                        ride.getStatus(),
                        null,
                        ride.getDriverId()
                );
            }
        }
    }

    private void updateRideStatusFromProvider(RideRequest ride, String providerStatus) {
        RideStatus newStatus = switch (providerStatus.toLowerCase()) {
            case "accepted", "driver_assigned" -> RideStatus.ACCEPTED;
            case "arriving", "in_progress" -> RideStatus.IN_PROGRESS;
            case "completed" -> RideStatus.COMPLETED;
            case "cancelled" -> RideStatus.CANCELLED;
            default -> ride.getStatus();
        };

        if (newStatus != ride.getStatus()) {
            ride.setStatus(newStatus);
            rideRepo.save(ride);
            // Update cache
            cacheService.set("ride:" + ride.getRideRequestId(), ride, Duration.ofHours(24));
        }
    }

    /**
     * Cancel an existing ride.
     */
    @Transactional
    public CancelRideResponse cancelRide(UUID rideId) {
        RideRequest ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride","id",rideId));

        if (ride.getStatus() == RideStatus.CANCELLED
                || ride.getStatus() == RideStatus.COMPLETED) {
            return new CancelRideResponse(false, "Ride is already " + ride.getStatus());
        }

        boolean cancelled = false;

        if (ride.getProviderType() == ProviderType.INTERNAL) {
            ride.setStatus(RideStatus.CANCELLED);
            cancelled = true;

            // Make driver available again
            if (ride.getDriverId() != null) {
                driverRepo.findById(ride.getDriverId()).ifPresent(driver -> {
                    driver.setAvailable(true);
                    driverRepo.save(driver);
                });
            }
        } else {
            // Cancel with external provider
            try {
                rideProviderService.cancelRide(
                        ride.getProviderType().toString(),
                        ride.getExternalRideId()
                ).block();

                ride.setStatus(RideStatus.CANCELLED);
                cancelled = true;
            } catch (Exception e) {
                log.error("Failed to cancel ride with provider: {}", ride.getProviderType(), e);
                return new CancelRideResponse(false, "Failed to cancel with provider");
            }
        }

        if (cancelled) {
            ride.setCancelTime(Instant.now());
            rideRepo.save(ride);

            // Update cache
            cacheService.set("ride:" + rideId, ride, Duration.ofHours(24));

            // Publish cancellation event
            rideEventService.publishRideCancelled(
                    rideId.toString(),
                    "CUSTOMER",
                    "Customer requested cancellation"
            );

            log.info("Ride {} cancelled successfully", rideId);
            return new CancelRideResponse(true, null);
        }

        return new CancelRideResponse(false, "Unable to cancel ride");
    }

    @Transactional
    public RideCompleteResponse completeRide(UUID rideId) throws StripeException {
        RideRequest ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride","id",rideId));

        if (ride.getStatus() == RideStatus.COMPLETED) {
            return new RideCompleteResponse(ride.getRideRequestId(), ride.getActualFare(), ride.getCurrency());
        }

        // mark complete
        ride.setCompleteTime(Instant.now());
        ride.setStatus(RideStatus.COMPLETED);

        // calculate fare
        double distKm = distance(
                ride.getPickupLatitude(), ride.getPickupLongitude(),
                ride.getDropoffLatitude(), ride.getDropoffLongitude()
        );
        long mins = Duration.between(ride.getRequestTime(), ride.getCompleteTime()).toMinutes();
        double fare = 2.0 + distKm * 1.0 + mins * 0.5; // base+per-km+per-min
        ride.setEstimatedFare(fare);
        ride.setActualFare(fare);
        ride.setCurrency("USD");

        // charge customer
        try {
            var paymentResult = paymentService.chargeCustomer(ride.getCustomerId(), fare, "usd");

            // Publish payment processed event
            rideEventService.publishPaymentProcessed(
                    rideId.toString(),
                    paymentResult.get("paymentId").toString(),
                    fare,
                    "USD",
                    "COMPLETED"
            );

        } catch (Exception e) {
            log.error("Failed to process payment for ride {}", rideId, e);

            // Publish payment failed event
            rideEventService.publishPaymentProcessed(
                    rideId.toString(),
                    null,
                    fare,
                    "USD",
                    "FAILED"
            );

            throw e;
        }

        // Make driver available again
        if (ride.getDriverId() != null) {
            driverRepo.findById(ride.getDriverId()).ifPresent(driver -> {
                driver.setAvailable(true);
                driverRepo.save(driver);
            });
        }

        rideRepo.save(ride);

        // Update cache
        cacheService.set("ride:" + rideId, ride, Duration.ofHours(24));

        // Publish ride completed event
        rideEventService.publishRideCompleted(
                rideId.toString(),
                distKm,
                mins,
                fare,
                "USD"
        );

        log.info("Ride {} completed successfully. Distance: {}km, Duration: {}min, Fare: ${}",
                rideId, distKm, mins, fare);

        return new RideCompleteResponse(ride.getRideRequestId(), fare, "USD");
    }

    /**
     * Get ride from cache (helper method for controller)
     */
    public RideRequest getRideFromCache(UUID rideId) {
        return cacheService.get("ride:" + rideId, RideRequest.class)
                .orElseGet(() -> rideRepo.findById(rideId).orElse(null));
    }

    // Internal dispatch: pick nearest available driver
    private DriverProfile dispatchInternalRide(RideRequest ride) {
        var available = driverRepo.findByAvailableTrue();
        if (available.isEmpty()) {
            throw new BadRequestException("No available drivers");
        }
        DriverProfile nearest = available.stream()
                .min(Comparator.comparingDouble(
                        d -> distance(ride.getPickupLatitude(), ride.getPickupLongitude(),
                                d.getCurrentLatitude(), d.getCurrentLongitude())
                )).get();

        nearest.setAvailable(false);
        driverRepo.save(nearest);
        return nearest;
    }

    // Haversine formula
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
