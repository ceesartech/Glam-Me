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
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.Ride;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repository.RideRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Comprehensive service for tracking and managing driver status, availability, and performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverTrackingService {

    private final DriverProfileRepository driverRepository;
    private final RideRepository rideRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;

    /**
     * Update driver status and availability
     */
    @Transactional
    public void updateDriverStatus(String driverId, String status, Boolean available, LocationDto location) {
        log.info("Updating driver status - Driver: {}, Status: {}, Available: {}", driverId, status, available);

        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            driver.setAvailable(available);
            driver.setOnline(available); // Assume online when available
            driver.setLastStatusUpdate(LocalDateTime.now());

            if (location != null) {
                driver.setCurrentLatitude(location.getLatitude());
                driver.setCurrentLongitude(location.getLongitude());
                driver.setLastLocationUpdate(LocalDateTime.now());
            }

            driverRepository.save(driver);

            // Broadcast driver status update
            DriverTrackingDto trackingDto = DriverTrackingDto.builder()
                    .driverId(driverId)
                    .status(status)
                    .online(available)
                    .lastUpdated(LocalDateTime.now())
                    .currentLocation(location)
                    .build();

            messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/status", trackingDto);
            messagingTemplate.convertAndSend("/topic/drivers/status", trackingDto);

            // Publish driver status event
            eventPublisher.publishEvent("glamme-bus",
                    java.util.Map.of(
                            "driverId", driverId,
                            "status", status,
                            "available", available.toString(),
                            "latitude", location != null ? location.getLatitude().toString() : "0.0",
                            "longitude", location != null ? location.getLongitude().toString() : "0.0",
                            "timestamp", LocalDateTime.now().toString()
                    ));

            log.info("Driver status updated: {}", driverId);
        }
    }

    /**
     * Start driver shift
     */
    @Transactional
    public void startDriverShift(String driverId, String vehicleId) {
        log.info("Starting driver shift - Driver: {}, Vehicle: {}", driverId, vehicleId);

        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            driver.setOnline(true);
            driver.setAvailable(true);
            driver.setShiftStartTime(LocalDateTime.now());
            driver.setVehicleId(vehicleId);
            driver.setRidesCompletedToday(0);
            driver.setEarningsToday(BigDecimal.ZERO);
            driver.setDistanceTraveledToday(BigDecimal.ZERO);
            driver.setHoursOnlineToday(0);

            driverRepository.save(driver);

            // Publish shift start event
            eventPublisher.publishEvent("glamme-bus",
                    java.util.Map.of(
                            "driverId", driverId,
                            "vehicleId", vehicleId,
                            "timestamp", LocalDateTime.now().toString()
                    ));

            log.info("Driver shift started: {}", driverId);
        }
    }

    /**
     * End driver shift
     */
    @Transactional
    public void endDriverShift(String driverId) {
        log.info("Ending driver shift - Driver: {}", driverId);

        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            driver.setOnline(false);
            driver.setAvailable(false);
            driver.setShiftEndTime(LocalDateTime.now());

            // Calculate hours online
            if (driver.getShiftStartTime() != null) {
                int hoursOnline = java.time.Duration.between(
                        driver.getShiftStartTime(),
                        driver.getShiftEndTime()
                ).toHoursPart();
                driver.setHoursOnlineToday(hoursOnline);
            }

            driverRepository.save(driver);

            // Publish shift end event
            eventPublisher.publishEvent("glamme-bus",
                    java.util.Map.of(
                            "driverId", driverId,
                            "earnings", driver.getEarningsToday() != null ? driver.getEarningsToday().toString() : "0",
                            "ridesCompleted", driver.getRidesCompletedToday() != null ? driver.getRidesCompletedToday().toString() : "0",
                            "hoursOnline", driver.getHoursOnlineToday() != null ? driver.getHoursOnlineToday().toString() : "0",
                            "timestamp", LocalDateTime.now().toString()
                    ));

            log.info("Driver shift ended: {}", driverId);
        }
    }

    /**
     * Get comprehensive driver status
     */
    public Optional<DriverTrackingDto> getDriverStatus(String driverId) {
        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            // Get current ride if any
            String currentRideId = null;
            List<Ride> activeRides = rideRepository.findByDriverIdAndStatus(driverId, Ride.Status.STARTED);
            if (!activeRides.isEmpty()) {
                currentRideId = activeRides.get(0).getRideId();
            }

            DriverTrackingDto trackingDto = DriverTrackingDto.builder()
                    .driverId(driverId)
                    .rideId(currentRideId)
                    .currentLocation(LocationDto.builder()
                            .latitude(driver.getCurrentLatitude())
                            .longitude(driver.getCurrentLongitude())
                            .build())
                    .status(driver.getAvailable() ? "AVAILABLE" : "BUSY")
                    .lastUpdated(driver.getLastLocationUpdate())
                    .lastRideCompleted(driver.getLastRideCompleted())
                    .ridesCompletedToday(driver.getRidesCompletedToday())
                    .earningsToday(driver.getEarningsToday())
                    .rating(driver.getRating())
                    .online(driver.getOnline())
                    .vehicleId(driver.getVehicleId())
                    .vehicleType(driver.getVehicleModel())
                    .shiftStartTime(driver.getShiftStartTime())
                    .shiftEndTime(driver.getShiftEndTime())
                    .distanceTraveledToday(driver.getDistanceTraveledToday())
                    .hoursOnlineToday(driver.getHoursOnlineToday())
                    .build();

            return Optional.of(trackingDto);
        }
        return Optional.empty();
    }

    /**
     * Get all available drivers in an area
     */
    public List<DriverTrackingDto> getAvailableDriversInArea(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        List<DriverProfile> allDrivers = driverRepository.findAll();

        return allDrivers.stream()
                .filter(driver -> driver.getAvailable() && driver.getOnline())
                .filter(driver -> isWithinRadius(
                        latitude.doubleValue(),
                        longitude.doubleValue(),
                        driver.getCurrentLatitude().doubleValue(),
                        driver.getCurrentLongitude().doubleValue(),
                        radiusKm))
                .map(driver -> DriverTrackingDto.builder()
                        .driverId(driver.getDriverId())
                        .currentLocation(LocationDto.builder()
                                .latitude(driver.getCurrentLatitude())
                                .longitude(driver.getCurrentLongitude())
                                .build())
                        .status("AVAILABLE")
                        .rating(driver.getRating())
                        .vehicleType(driver.getVehicleModel())
                        .lastUpdated(driver.getLastLocationUpdate())
                        .online(true)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Update driver earnings and statistics
     */
    @Transactional
    public void updateDriverEarnings(String driverId, BigDecimal fare, BigDecimal distance) {
        log.info("Updating driver earnings - Driver: {}, Fare: {}, Distance: {}", driverId, fare, distance);

        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            // Update daily statistics
            driver.setEarningsToday(driver.getEarningsToday().add(fare));
            driver.setDistanceTraveledToday(driver.getDistanceTraveledToday().add(distance));
            driver.setRidesCompletedToday(driver.getRidesCompletedToday() + 1);
            driver.setLastRideCompleted(LocalDateTime.now());

            driverRepository.save(driver);

            log.info("Driver earnings updated: {}", driverId);
        }
    }

    /**
     * Monitor driver activity and send alerts if needed
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorDriverActivity() {
        log.info("Monitoring driver activity");

        List<DriverProfile> activeDrivers = driverRepository.findByAvailableTrue();

        for (DriverProfile driver : activeDrivers) {
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

            // Check if driver has been inactive for too long
            if (driver.getLastLocationUpdate() != null &&
                driver.getLastLocationUpdate().isBefore(tenMinutesAgo)) {

                log.warn("Driver {} has been inactive for more than 10 minutes", driver.getDriverId());

                // Publish driver inactivity alert
                eventPublisher.publishEvent("glamme-bus",
                        java.util.Map.of(
                                "driverId", driver.getDriverId(),
                                "lastActivity", driver.getLastLocationUpdate().toString(),
                                "minutesInactive", "10",
                                "timestamp", LocalDateTime.now().toString()
                        ));
            }
        }
    }

    /**
     * Send driver performance report
     */
    @Scheduled(cron = "0 0 6 * * ?") // Daily at 6 AM
    public void sendDriverPerformanceReports() {
        log.info("Sending daily driver performance reports");

        List<DriverProfile> allDrivers = driverRepository.findAll();

        for (DriverProfile driver : allDrivers) {
            if (driver.getRidesCompletedToday() > 0) {
                // Publish performance report
                eventPublisher.publishEvent("glamme-bus",
                        java.util.Map.of(
                                "driverId", driver.getDriverId(),
                                "ridesCompleted", driver.getRidesCompletedToday().toString(),
                                "earnings", driver.getEarningsToday().toString(),
                                "distance", driver.getDistanceTraveledToday().toString(),
                                "hoursOnline", driver.getHoursOnlineToday().toString(),
                                "rating", driver.getRating() != null ? driver.getRating().toString() : "0",
                                "reportDate", LocalDateTime.now().toString()
                        ));

                // Reset daily statistics
                resetDriverDailyStats(driver);
            }
        }
    }

    /**
     * Reset driver's daily statistics
     */
    private void resetDriverDailyStats(DriverProfile driver) {
        driver.setRidesCompletedToday(0);
        driver.setEarningsToday(BigDecimal.ZERO);
        driver.setDistanceTraveledToday(BigDecimal.ZERO);
        driver.setHoursOnlineToday(0);
        driverRepository.save(driver);
    }

    /**
     * Check if a location is within a radius of another location
     */
    private boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance <= radiusKm;
    }

    /**
     * Get driver performance metrics
     */
    public DriverTrackingDto getDriverPerformanceMetrics(String driverId) {
        Optional<DriverProfile> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isPresent()) {
            DriverProfile driver = driverOpt.get();

            // Calculate performance metrics
            long totalRides = rideRepository.countByDriverIdAndStatus(driverId, Ride.Status.COMPLETED);
            long cancelledRides = rideRepository.countByDriverIdAndStatus(driverId, Ride.Status.CANCELLED);
            double completionRate = totalRides > 0 ? (double) totalRides / (totalRides + cancelledRides) * 100 : 0;

            return DriverTrackingDto.builder()
                    .driverId(driverId)
                    .ridesCompletedToday(driver.getRidesCompletedToday())
                    .earningsToday(driver.getEarningsToday())
                    .rating(driver.getRating())
                    .distanceTraveledToday(driver.getDistanceTraveledToday())
                    .hoursOnlineToday(driver.getHoursOnlineToday())
                    .build();
        }
        return null;
    }
}
