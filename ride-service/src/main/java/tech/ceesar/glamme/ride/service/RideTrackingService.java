package tech.ceesar.glamme.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.ride.dto.DriverTrackingDto;
import tech.ceesar.glamme.ride.dto.LocationDto;
import tech.ceesar.glamme.ride.dto.RideTrackingDto;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.Ride;
import tech.ceesar.glamme.ride.entity.RideTracking;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repository.RideRepository;
import tech.ceesar.glamme.ride.repository.RideTrackingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Comprehensive service for tracking rides, drivers, and real-time location updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideTrackingService {

    private final RideRepository rideRepository;
    private final RideTrackingRepository rideTrackingRepository;
    private final DriverProfileRepository driverRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;

    /**
     * Update driver location and broadcast to subscribers
     */
    @Transactional
    public void updateDriverLocation(String driverId, LocationDto location, Integer heading,
                                   BigDecimal speed, BigDecimal accuracy) {
        log.info("Updating location for driver: {} - Lat: {}, Lng: {}",
                driverId, location.getLatitude(), location.getLongitude());

        // Update driver profile location
        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();
            driver.setCurrentLatitude(location.getLatitude());
            driver.setCurrentLongitude(location.getLongitude());
            driver.setLastLocationUpdate(LocalDateTime.now());
            driverRepository.save(driver);
        }

        // Create tracking record
        RideTracking tracking = RideTracking.builder()
                .driverId(driverId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .heading(heading)
                .speedMph(speed)
                .accuracyMeters(accuracy)
                .timestamp(LocalDateTime.now())
                .build();

        rideTrackingRepository.save(tracking);

        // Broadcast location update via WebSocket
        DriverTrackingDto trackingDto = DriverTrackingDto.builder()
                .driverId(driverId)
                .currentLocation(location)
                .heading(heading)
                .speedMph(speed)
                .accuracyMeters(accuracy)
                .lastUpdated(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/location", trackingDto);

        // Publish location update event
        eventPublisher.publishEvent("glamme-bus",
                java.util.Map.of(
                        "driverId", driverId,
                        "latitude", location.getLatitude().toString(),
                        "longitude", location.getLongitude().toString(),
                        "heading", heading != null ? heading.toString() : "0",
                        "speed", speed != null ? speed.toString() : "0",
                        "timestamp", LocalDateTime.now().toString()
                ));

        log.info("Driver location updated and broadcasted: {}", driverId);
    }

    /**
     * Update ride tracking with current location and progress
     */
    @Transactional
    public void updateRideProgress(String rideId, String driverId, LocationDto currentLocation,
                                 BigDecimal distanceTraveled, Integer etaToDestination) {
        log.info("Updating ride progress - Ride: {}, Driver: {}, Distance: {} miles",
                rideId, driverId, distanceTraveled);

        Optional<Ride> rideOpt = rideRepository.findByRideId(rideId);
        if (rideOpt.isPresent()) {
            Ride ride = rideOpt.get();

            // Update ride with current progress
            ride.setActualDistanceMiles(distanceTraveled);
            ride.setUpdatedAt(LocalDateTime.now());
            rideRepository.save(ride);

            // Create tracking record
            RideTracking tracking = RideTracking.builder()
                    .rideId(rideId)
                    .driverId(driverId)
                    .latitude(currentLocation.getLatitude())
                    .longitude(currentLocation.getLongitude())
                    .timestamp(LocalDateTime.now())
                    .build();

            rideTrackingRepository.save(tracking);

            // Broadcast ride progress update
            RideTrackingDto progressDto = RideTrackingDto.builder()
                    .rideId(rideId)
                    .driverId(driverId)
                    .currentLocation(currentLocation)
                    .distanceTraveled(distanceTraveled)
                    .etaToDestination(etaToDestination)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/progress", progressDto);

            // Publish ride progress event
            eventPublisher.publishEvent("glamme-bus",
                    java.util.Map.of(
                            "rideId", rideId,
                            "driverId", driverId,
                            "latitude", currentLocation.getLatitude().toString(),
                            "longitude", currentLocation.getLongitude().toString(),
                            "distanceTraveled", distanceTraveled.toString(),
                            "etaToDestination", etaToDestination != null ? etaToDestination.toString() : "0",
                            "timestamp", LocalDateTime.now().toString()
                    ));

            log.info("Ride progress updated: {}", rideId);
        }
    }

    /**
     * Get driver's current location and status
     */
    public Optional<DriverTrackingDto> getDriverLocation(String driverId) {
        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            DriverTrackingDto trackingDto = DriverTrackingDto.builder()
                    .driverId(driverId)
                    .currentLocation(LocationDto.builder()
                            .latitude(driver.getCurrentLatitude())
                            .longitude(driver.getCurrentLongitude())
                            .build())
                    .lastUpdated(driver.getLastLocationUpdate())
                    .status(driver.getAvailable() ? "AVAILABLE" : "BUSY")
                    .online(driver.getOnline())
                    .rating(driver.getRating())
                    .build();

            return Optional.of(trackingDto);
        }
        return Optional.empty();
    }

    /**
     * Get ride tracking history
     */
    public List<RideTrackingDto> getRideTrackingHistory(String rideId) {
        List<RideTracking> trackingRecords = rideTrackingRepository.findByRideIdOrderByTimestampDesc(rideId);

        return trackingRecords.stream()
                .map(record -> RideTrackingDto.builder()
                        .rideId(record.getRideId())
                        .driverId(record.getDriverId())
                        .currentLocation(LocationDto.builder()
                                .latitude(record.getLatitude())
                                .longitude(record.getLongitude())
                                .build())
                        .heading(record.getHeading())
                        .speedMph(record.getSpeedMph())
                        .accuracyMeters(record.getAccuracyMeters())
                        .timestamp(record.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get driver's tracking history for a time period
     */
    public List<RideTrackingDto> getDriverTrackingHistory(String driverId, LocalDateTime startTime, LocalDateTime endTime) {
        List<RideTracking> trackingRecords = rideTrackingRepository
                .findByDriverIdAndTimestampBetween(driverId, startTime, endTime);

        return trackingRecords.stream()
                .map(record -> RideTrackingDto.builder()
                        .rideId(record.getRideId())
                        .driverId(record.getDriverId())
                        .currentLocation(LocationDto.builder()
                                .latitude(record.getLatitude())
                                .longitude(record.getLongitude())
                                .build())
                        .heading(record.getHeading())
                        .speedMph(record.getSpeedMph())
                        .accuracyMeters(record.getAccuracyMeters())
                        .timestamp(record.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get latest tracking information for a ride
     */
    public Optional<RideTrackingDto> getLatestRideTracking(String rideId) {
        Optional<RideTracking> latestTracking = rideTrackingRepository.findLatestByRideId(rideId);

        return latestTracking.map(record -> RideTrackingDto.builder()
                .rideId(record.getRideId())
                .driverId(record.getDriverId())
                .currentLocation(LocationDto.builder()
                        .latitude(record.getLatitude())
                        .longitude(record.getLongitude())
                        .build())
                .heading(record.getHeading())
                .speedMph(record.getSpeedMph())
                .accuracyMeters(record.getAccuracyMeters())
                .timestamp(record.getTimestamp())
                .build());
    }

    /**
     * Monitor and update driver status based on activity
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorDriverActivity() {
        log.info("Monitoring driver activity and updating status");

        List<DriverProfile> allDrivers = driverRepository.findAll();

        for (DriverProfile driver : allDrivers) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

            // Check if driver has updated location recently
            if (driver.getLastLocationUpdate() != null &&
                driver.getLastLocationUpdate().isBefore(fiveMinutesAgo)) {

                // Mark driver as potentially offline
                driver.setOnline(false);
                driverRepository.save(driver);

                log.warn("Driver {} marked as offline due to inactivity", driver.getDriverId());
            }
        }
    }

    /**
     * Clean up old tracking records (older than 30 days)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldTrackingRecords() {
        log.info("Cleaning up old tracking records");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<RideTracking> oldRecords = rideTrackingRepository
                .findByDriverIdAndTimestampBetween("all", LocalDateTime.now().minusYears(1), thirtyDaysAgo);

        if (!oldRecords.isEmpty()) {
            rideTrackingRepository.deleteAll(oldRecords);
            log.info("Cleaned up {} old tracking records", oldRecords.size());
        }
    }

    /**
     * Calculate distance between two locations using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to km

        return distance * 0.621371; // convert to miles
    }

    /**
     * Estimate ETA based on current location and destination
     */
    public Integer estimateETA(LocationDto currentLocation, LocationDto destination, double averageSpeedMph) {
        double distance = calculateDistance(
                currentLocation.getLatitude().doubleValue(),
                currentLocation.getLongitude().doubleValue(),
                destination.getLatitude().doubleValue(),
                destination.getLongitude().doubleValue()
        );

        // Estimate time in hours, then convert to minutes
        double timeInHours = distance / averageSpeedMph;
        return (int) Math.round(timeInHours * 60);
    }
}
