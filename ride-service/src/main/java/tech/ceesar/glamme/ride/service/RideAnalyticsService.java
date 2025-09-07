package tech.ceesar.glamme.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.ride.dto.DriverTrackingDto;
import tech.ceesar.glamme.ride.dto.RideAnalyticsDto;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.Ride;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repository.RideRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive analytics service for ride data analysis and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideAnalyticsService {

    private final RideRepository rideRepository;
    private final DriverProfileRepository driverRepository;

    /**
     * Get comprehensive ride analytics for a date range
     */
    @Cacheable(value = "rideAnalytics", key = "#startDate.toString() + '_' + #endDate.toString()")
    @Transactional(readOnly = true)
    public RideAnalyticsDto getRideAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating ride analytics for period: {} to {}", startDate, endDate);

        List<RideRequest> rides = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.COMPLETED.toString(), startDate, endDate);

        // Calculate basic metrics
        long totalRides = rides.size();
        long completedRides = rides.stream()
                .filter(ride -> ride.getStatus().equals(RideStatus.COMPLETED.toString()))
                .count();
        long cancelledRides = rides.stream()
                .filter(ride -> ride.getStatus().equals(RideStatus.CANCELLED.toString()))
                .count();

        // Calculate financial metrics
        BigDecimal totalRevenue = rides.stream()
                .filter(ride -> ride.getActualFare() > 0)
                .map(ride -> BigDecimal.valueOf(ride.getActualFare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageFare = totalRides > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(totalRides), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate driver metrics
        List<DriverProfile> allDrivers = driverRepository.findAll();
        long activeDrivers = allDrivers.stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getAvailable()))
                .count();

        BigDecimal totalDriverEarnings = calculateTotalDriverEarnings(rides);
        BigDecimal averageDriverRating = calculateAverageDriverRating(allDrivers);

        // Calculate time-based metrics
        LocalDateTime now = LocalDateTime.now();
        long ridesToday = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.COMPLETED.toString(),
                now.toLocalDate().atStartOfDay(),
                now.toLocalDate().atTime(23, 59, 59)).size();

        long ridesThisWeek = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.COMPLETED.toString(),
                now.minusDays(7),
                now).size();

        long ridesThisMonth = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.COMPLETED.toString(),
                now.minusDays(30),
                now).size();

        // Provider breakdown
        Map<ProviderType, Long> providerStats = rides.stream()
                .collect(Collectors.groupingBy(ride -> ride.getProviderType(), Collectors.counting()));

        return RideAnalyticsDto.builder()
                .totalRides(totalRides)
                .completedRides(completedRides)
                .cancelledRides(cancelledRides)
                .completionRate(BigDecimal.valueOf(totalRides > 0 ? (double) completedRides / totalRides * 100 : 0.0))
                .totalRevenue(totalRevenue)
                .averageFare(averageFare)
                .totalDriverEarnings(totalDriverEarnings)
                .platformFee(calculatePlatformFee(totalRevenue))
                .averageRideDuration(calculateAverageRideDuration(rides))
                .averageDistance(calculateAverageDistance(rides))
                .averageRating(calculateAverageRating(rides).doubleValue())
                .ridesToday(ridesToday)
                .ridesThisWeek(ridesThisWeek)
                .ridesThisMonth(ridesThisMonth)
                .revenueToday(calculateRevenueForPeriod(
                        now.toLocalDate().atStartOfDay(),
                        now.toLocalDate().atTime(23, 59, 59)))
                .revenueThisWeek(calculateRevenueForPeriod(now.minusDays(7), now))
                .revenueThisMonth(calculateRevenueForPeriod(now.minusDays(30), now))
                .uberRides(providerStats.getOrDefault(ProviderType.UBER, 0L))
                .lyftRides(providerStats.getOrDefault(ProviderType.LYFT, 0L))
                .internalRides(providerStats.getOrDefault(ProviderType.INTERNAL, 0L))
                .activeDrivers(activeDrivers)
                .averageDriverRating(averageDriverRating.doubleValue())
                .uniqueCustomers(calculateUniqueCustomers(rides))
                .build();
    }

    /**
     * Get driver performance analytics
     */
    @Cacheable(value = "driverAnalytics", key = "#driverId + '_' + #startDate.toString() + '_' + #endDate.toString()")
    @Transactional(readOnly = true)
    public DriverAnalyticsDto getDriverAnalytics(String driverId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating driver analytics for driver: {} from {} to {}", driverId, startDate, endDate);

        List<RideRequest> driverRides = rideRepository.findByDriverIdAndStatus(driverId, RideStatus.COMPLETED.toString());

        // Filter by date range
        List<RideRequest> ridesInPeriod = driverRides.stream()
                  .filter(ride -> ride.getRequestTime().isAfter(java.time.Instant.from(startDate.atZone(java.time.ZoneId.systemDefault()))) && ride.getRequestTime().isBefore(java.time.Instant.from(endDate.atZone(java.time.ZoneId.systemDefault()))))
                .collect(Collectors.toList());

        long totalRides = ridesInPeriod.size();
        BigDecimal totalEarnings = ridesInPeriod.stream()
                .filter(ride -> ride.getActualFare() > 0)
                .map(ride -> BigDecimal.valueOf(ride.getActualFare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageFare = totalRides > 0 ?
                totalEarnings.divide(BigDecimal.valueOf(totalRides), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        double totalDistance = ridesInPeriod.stream()
                .filter(ride -> true) // Distance not tracked in RideRequest
                .mapToDouble(ride -> 5.0) // Default distance in miles
                .sum();

        double averageDistance = totalRides > 0 ? totalDistance / totalRides : 0.0;

        return new DriverAnalyticsDto(
                driverId,
                totalRides,
                totalEarnings,
                averageFare,
                BigDecimal.valueOf(totalDistance),
                BigDecimal.valueOf(averageDistance),
                calculateAverageRating(ridesInPeriod),
                calculateCompletionRate(driverRides),
                startDate,
                endDate
        );
    }

    /**
     * Get revenue analytics by time period
     */
    @Transactional(readOnly = true)
    public RevenueAnalyticsDto getRevenueAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating revenue analytics from {} to {}", startDate, endDate);

        List<RideRequest> completedRides = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.COMPLETED.toString(), startDate, endDate);

        BigDecimal totalRevenue = completedRides.stream()
                .filter(ride -> ride.getActualFare() > 0)
                .map(ride -> BigDecimal.valueOf(ride.getActualFare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformRevenue = calculatePlatformFee(totalRevenue);
        BigDecimal driverRevenue = totalRevenue.subtract(platformRevenue);

        // Daily breakdown
        Map<String, BigDecimal> dailyRevenue = completedRides.stream()
                .filter(ride -> ride.getActualFare() > 0)
                .collect(Collectors.groupingBy(
                        ride -> ride.getRequestTime().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, ride -> BigDecimal.valueOf(ride.getActualFare()), BigDecimal::add)
                ));

        return new RevenueAnalyticsDto(
                totalRevenue,
                platformRevenue,
                driverRevenue,
                dailyRevenue,
                startDate,
                endDate
        );
    }

    /**
     * Get operational metrics for monitoring
     */
    @Transactional(readOnly = true)
    public OperationalMetricsDto getOperationalMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59);

        // Current active rides
        List<RideRequest> activeRides = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.STARTED.toString(), todayStart, todayEnd);

        // Pending ride requests
        List<RideRequest> pendingRides = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.REQUESTED.toString(), todayStart, todayEnd);

        // Available drivers
        List<DriverProfile> availableDrivers = driverRepository.findByAvailableTrue();

        // Calculate average wait time for pending rides
        double averageWaitTime = calculateAverageWaitTime(pendingRides, now);

        return new OperationalMetricsDto(
                activeRides.size(),
                pendingRides.size(),
                availableDrivers.size(),
                averageWaitTime,
                calculateAverageRideDuration(activeRides),
                now
        );
    }

    /**
     * Get geographical analytics for ride demand patterns
     */
    @Transactional(readOnly = true)
    public GeographicalAnalyticsDto getGeographicalAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating geographical analytics from {} to {}", startDate, endDate);

        List<RideRequest> rides = rideRepository.findRidesByStatusAndDateRange(
                RideStatus.COMPLETED.toString(), startDate, endDate);

        // Group by pickup location (simplified - in production you'd use geo-hashing)
        Map<String, Long> pickupHotspots = rides.stream()
                .filter(ride -> ride.getPickupLatitude() != 0.0 && ride.getPickupLongitude() != 0.0)
                .collect(Collectors.groupingBy(
                        ride -> String.format("%.2f,%.2f",
                                ride.getPickupLatitude(),
                                ride.getPickupLongitude()),
                        Collectors.counting()
                ));

        Map<String, Long> dropoffHotspots = rides.stream()
                .filter(ride -> ride.getDropoffLatitude() != 0.0 && ride.getDropoffLongitude() != 0.0)
                .collect(Collectors.groupingBy(
                        ride -> String.format("%.2f,%.2f",
                                ride.getDropoffLatitude(),
                                ride.getDropoffLongitude()),
                        Collectors.counting()
                ));

        return new GeographicalAnalyticsDto(
                pickupHotspots,
                dropoffHotspots,
                calculateMostPopularRoutes(rides),
                startDate,
                endDate
        );
    }

    // Helper methods

    private BigDecimal calculateTotalDriverEarnings(List<RideRequest> rides) {
        return rides.stream()
                .filter(ride -> ride.getActualFare() > 0)
                .map(ride -> BigDecimal.valueOf(ride.getActualFare() * 0.8)) // 80% to driver
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePlatformFee(BigDecimal totalRevenue) {
        return totalRevenue.multiply(BigDecimal.valueOf(0.2)); // 20% platform fee
    }

    private double calculateAverageRideDuration(List<RideRequest> rides) {
        return rides.stream()
                .filter(ride -> true) // Duration not tracked in RideRequest
                .mapToInt(ride -> 0) // Duration not available in RideRequest entity
                .average()
                .orElse(0.0);
    }

    private double calculateAverageDistance(List<RideRequest> rides) {
        return rides.stream()
                .filter(ride -> true) // Distance not tracked in RideRequest
                .mapToDouble(ride -> 5.0) // Default distance in miles
                .average()
                .orElse(0.0);
    }

    private BigDecimal calculateAverageRating(List<RideRequest> rides) {
        // Rating data not available in RideRequest entity
        // This would typically come from a separate review/rating service
        return BigDecimal.valueOf(4.5); // Default average rating
    }

    private BigDecimal calculateAverageDriverRating(List<DriverProfile> drivers) {
        return drivers.stream()
                .filter(driver -> driver.getRating() != null)
                .map(DriverProfile::getRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(drivers.size()), 2, RoundingMode.HALF_UP);
    }

    private long calculateUniqueCustomers(List<RideRequest> rides) {
        return rides.stream()
                .map(ride -> ride.getCustomerId().toString())
                .distinct()
                .count();
    }

    private double calculateCompletionRate(List<RideRequest> rides) {
        if (rides.isEmpty()) return 0.0;
        long completed = rides.stream()
                .filter(ride -> ride.getStatus().equals(RideStatus.COMPLETED.toString()))
                .count();
        return (double) completed / rides.size() * 100;
    }

    private BigDecimal calculateRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        List<RideRequest> rides = rideRepository.findRidesByStatusAndDateRange(RideStatus.COMPLETED.toString(), start, end);
        return rides.stream()
                .filter(ride -> ride.getActualFare() > 0)
                .map(ride -> BigDecimal.valueOf(ride.getActualFare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculateAverageWaitTime(List<RideRequest> pendingRides, LocalDateTime now) {
        return pendingRides.stream()
                .filter(ride -> ride.getRequestTime() != null)
                .mapToLong(ride -> java.time.Duration.between(ride.getRequestTime(), now).toMinutes())
                .average()
                .orElse(0.0);
    }

    private Map<String, Long> calculateMostPopularRoutes(List<RideRequest> rides) {
        return rides.stream()
                .filter(ride -> ride.getPickupLatitude() != 0.0 && ride.getPickupLongitude() != 0.0
                        && ride.getDropoffLatitude() != 0.0 && ride.getDropoffLongitude() != 0.0)
                .collect(Collectors.groupingBy(
                        ride -> String.format("%.2f,%.2f -> %.2f,%.2f",
                                ride.getPickupLatitude(),
                                ride.getPickupLongitude(),
                                ride.getDropoffLatitude(),
                                ride.getDropoffLongitude()),
                        Collectors.counting()
                ));
    }

    // DTO classes for analytics responses

    public static class DriverAnalyticsDto {
        private final String driverId;
        private final long totalRides;
        private final BigDecimal totalEarnings;
        private final BigDecimal averageFare;
        private final BigDecimal totalDistance;
        private final BigDecimal averageDistance;
        private final BigDecimal averageRating;
        private final double completionRate;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public DriverAnalyticsDto(String driverId, long totalRides, BigDecimal totalEarnings,
                                BigDecimal averageFare, BigDecimal totalDistance, BigDecimal averageDistance,
                                BigDecimal averageRating, double completionRate,
                                LocalDateTime startDate, LocalDateTime endDate) {
            this.driverId = driverId;
            this.totalRides = totalRides;
            this.totalEarnings = totalEarnings;
            this.averageFare = averageFare;
            this.totalDistance = totalDistance;
            this.averageDistance = averageDistance;
            this.averageRating = averageRating;
            this.completionRate = completionRate;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters
        public String getDriverId() { return driverId; }
        public long getTotalRides() { return totalRides; }
        public BigDecimal getTotalEarnings() { return totalEarnings; }
        public BigDecimal getAverageFare() { return averageFare; }
        public BigDecimal getTotalDistance() { return totalDistance; }
        public BigDecimal getAverageDistance() { return averageDistance; }
        public BigDecimal getAverageRating() { return averageRating; }
        public double getCompletionRate() { return completionRate; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
    }

    public static class RevenueAnalyticsDto {
        private final BigDecimal totalRevenue;
        private final BigDecimal platformRevenue;
        private final BigDecimal driverRevenue;
        private final Map<String, BigDecimal> dailyRevenue;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public RevenueAnalyticsDto(BigDecimal totalRevenue, BigDecimal platformRevenue,
                                 BigDecimal driverRevenue, Map<String, BigDecimal> dailyRevenue,
                                 LocalDateTime startDate, LocalDateTime endDate) {
            this.totalRevenue = totalRevenue;
            this.platformRevenue = platformRevenue;
            this.driverRevenue = driverRevenue;
            this.dailyRevenue = dailyRevenue;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public BigDecimal getPlatformRevenue() { return platformRevenue; }
        public BigDecimal getDriverRevenue() { return driverRevenue; }
        public Map<String, BigDecimal> getDailyRevenue() { return dailyRevenue; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
    }

    public static class OperationalMetricsDto {
        private final int activeRides;
        private final int pendingRides;
        private final int availableDrivers;
        private final double averageWaitTime;
        private final double averageRideDuration;
        private final LocalDateTime timestamp;

        public OperationalMetricsDto(int activeRides, int pendingRides, int availableDrivers,
                                   double averageWaitTime, double averageRideDuration, LocalDateTime timestamp) {
            this.activeRides = activeRides;
            this.pendingRides = pendingRides;
            this.availableDrivers = availableDrivers;
            this.averageWaitTime = averageWaitTime;
            this.averageRideDuration = averageRideDuration;
            this.timestamp = timestamp;
        }

        // Getters
        public int getActiveRides() { return activeRides; }
        public int getPendingRides() { return pendingRides; }
        public int getAvailableDrivers() { return availableDrivers; }
        public double getAverageWaitTime() { return averageWaitTime; }
        public double getAverageRideDuration() { return averageRideDuration; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class GeographicalAnalyticsDto {
        private final Map<String, Long> pickupHotspots;
        private final Map<String, Long> dropoffHotspots;
        private final Map<String, Long> popularRoutes;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public GeographicalAnalyticsDto(Map<String, Long> pickupHotspots, Map<String, Long> dropoffHotspots,
                                      Map<String, Long> popularRoutes, LocalDateTime startDate, LocalDateTime endDate) {
            this.pickupHotspots = pickupHotspots;
            this.dropoffHotspots = dropoffHotspots;
            this.popularRoutes = popularRoutes;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters
        public Map<String, Long> getPickupHotspots() { return pickupHotspots; }
        public Map<String, Long> getDropoffHotspots() { return dropoffHotspots; }
        public Map<String, Long> getPopularRoutes() { return popularRoutes; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
    }
}
