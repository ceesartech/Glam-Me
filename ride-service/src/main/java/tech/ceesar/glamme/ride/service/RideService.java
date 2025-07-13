package tech.ceesar.glamme.ride.service;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.ride.client.ExternalRideClient;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repositories.RideRequestRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideService {
    @Autowired
    private PaymentService paymentService;
    private final RideRequestRepository rideRepo;
    private final DriverProfileRepository driverRepo;
    private final Map<ProviderType, ExternalRideClient> externalClients;

    /**
     * Create a new ride, either internal or external.
     */
    @Transactional
    public CreateRideResponse createRide(CreateRideRequest req) {
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
        } else {
            // external
            ExternalRideClient client = externalClients.get(req.getProviderType());
            if (client == null) {
                throw new BadRequestException("Unsupported provider");
            }
            String extId = client.requestRide(req);
            ride.setExternalRideId(extId);
        }

        ride = rideRepo.save(ride);
        return new CreateRideResponse(ride.getRideRequestId(), ride.getExternalRideId());
    }

    /**
     * Get current ride status.
     */
    @Transactional(readOnly = true)
    public RideStatusResponse getStatus(UUID rideId) {
        RideRequest ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride","id",rideId));

        if (ride.getProviderType() == ProviderType.INTERNAL) {
            return new RideStatusResponse(
                    ride.getRideRequestId(),
                    ride.getProviderType(),
                    ride.getStatus(),
                    null,
                    ride.getDriverId()
            );
        } else {
            ExternalRideClient client = externalClients.get(ride.getProviderType());
            RideStatusResponse resp = client.getStatus(ride.getExternalRideId());
            resp.setRideId(rideId);
            return resp;
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
            return new CancelRideResponse(false);
        }

        if (ride.getProviderType() == ProviderType.INTERNAL) {
            ride.setStatus(RideStatus.CANCELLED);
        } else {
            ExternalRideClient client = externalClients.get(ride.getProviderType());
            boolean ok = client.cancelRide(ride.getExternalRideId());
            if (ok) ride.setStatus(RideStatus.CANCELLED);
            else return new CancelRideResponse(false);
        }
        ride.setCancelTime(Instant.now());
        rideRepo.save(ride);
        return new CancelRideResponse(true);
    }

    @Transactional
    public RideCompleteResponse completeRide(UUID rideId) throws StripeException {
        RideRequest ride = rideRepo.findById(rideId)
                .orElseThrow();
        if (ride.getStatus()==RideStatus.COMPLETED) {
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
        paymentService.chargeCustomer(ride.getCustomerId(), fare, "usd");

        rideRepo.save(ride);
        return new RideCompleteResponse(ride.getRideRequestId(), fare, "USD");
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
