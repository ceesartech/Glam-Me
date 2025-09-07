package tech.ceesar.glamme.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.ride.dto.LocationDto;
import tech.ceesar.glamme.ride.dto.RideDto;
import tech.ceesar.glamme.ride.dto.RideTrackingDto;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.entity.RideTracking;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repository.RideRepository;
import tech.ceesar.glamme.ride.repository.RideTrackingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing ride history and trip details
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideHistoryService {

    private final RideRepository rideRepository;
    private final RideTrackingRepository rideTrackingRepository;

    /**
     * Get paginated ride history for a customer
     */
    public RideHistoryPage getCustomerRideHistory(String customerId, int page, int size, String sortBy, String sortDir) {
        log.info("Getting ride history for customer: {}, page: {}, size: {}", customerId, page, size);

        // Convert string customerId to UUID and then back to string for repository
        UUID customerUUID = UUID.fromString(customerId);
        List<RideRequest> allRides = rideRepository.findByCustomerId(customerUUID.toString());

        // Manual pagination since we don't have direct pageable support
        int start = page * size;
        int end = Math.min(start + size, allRides.size());
        List<RideRequest> pageContent = allRides.subList(start, end);

        List<RideDto> rides = pageContent.stream()
                .map(this::mapToRideDto)
                .collect(Collectors.toList());

        return new RideHistoryPage(
                rides,
                allRides.size(),
                (int) Math.ceil((double) allRides.size() / size),
                page,
                size,
                page == 0,
                end >= allRides.size()
        );
    }

    /**
     * Get paginated ride history for a driver
     */
    public RideHistoryPage getDriverRideHistory(String driverId, int page, int size, String sortBy, String sortDir) {
        log.info("Getting ride history for driver: {}, page: {}, size: {}", driverId, page, size);

        // Convert string driverId to UUID and then back to string for repository
        UUID driverUUID = UUID.fromString(driverId);
        List<RideRequest> allRides = rideRepository.findByDriverId(driverUUID.toString());

        // Manual pagination since we don't have direct pageable support
        int start = page * size;
        int end = Math.min(start + size, allRides.size());
        List<RideRequest> pageContent = allRides.subList(start, end);

        List<RideDto> rides = pageContent.stream()
                .map(this::mapToRideDto)
                .collect(Collectors.toList());

        return new RideHistoryPage(
                rides,
                allRides.size(),
                (int) Math.ceil((double) allRides.size() / size),
                page,
                size,
                page == 0,
                end >= allRides.size()
        );
    }

    /**
     * Get detailed ride information including tracking data
     */
    public Optional<RideDetailsDto> getRideDetails(String rideId) {
        log.info("Getting ride details for ride: {}", rideId);

        Optional<RideRequest> rideOpt = rideRepository.findByRideId(rideId);
        if (rideOpt.isEmpty()) {
            return Optional.empty();
        }

        RideRequest ride = rideOpt.get();
        List<RideTracking> tracking = rideTrackingRepository.findByRideIdOrderByTimestampAsc(rideId);

        return Optional.of(new RideDetailsDto(
                mapToRideDto(ride),
                tracking.stream()
                        .map(this::mapToTrackingDto)
                        .collect(Collectors.toList())
        ));
    }

    /**
     * Get ride receipt with detailed pricing breakdown
     */
    public Optional<RideReceiptDto> getRideReceipt(String rideId) {
        log.info("Getting receipt for ride: {}", rideId);

        Optional<RideRequest> rideOpt = rideRepository.findByRideId(rideId);
        if (rideOpt.isEmpty()) {
            return Optional.empty();
        }

        RideRequest ride = rideOpt.get();

        // Calculate pricing breakdown using actualFare
        BigDecimal fare = BigDecimal.valueOf(ride.getActualFare());
        BigDecimal baseFare = fare.multiply(new BigDecimal("0.7")); // 70% base fare
        BigDecimal distanceFare = fare.multiply(new BigDecimal("0.2")); // 20% distance
        BigDecimal timeFare = fare.multiply(new BigDecimal("0.1")); // 10% time
        BigDecimal tax = fare.multiply(new BigDecimal("0.08")); // 8% tax
        BigDecimal total = fare.add(tax);

        // Create location strings from coordinates
        String pickupLocation = String.format("%.6f,%.6f", ride.getPickupLatitude(), ride.getPickupLongitude());
        String dropoffLocation = String.format("%.6f,%.6f", ride.getDropoffLatitude(), ride.getDropoffLongitude());

        return Optional.of(new RideReceiptDto(
                rideId,
                ride.getCustomerId() != null ? ride.getCustomerId().toString() : null,
                ride.getDriverId() != null ? ride.getDriverId().toString() : null,
                ride.getRequestTime() != null ? ride.getRequestTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                ride.getCompleteTime() != null ? ride.getCompleteTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                pickupLocation,
                dropoffLocation,
                baseFare,
                distanceFare,
                timeFare,
                tax,
                fare,
                total,
                "Credit Card",
                "**** **** **** 1234"
        ));
    }

    /**
     * Get user ride statistics
     */
    public RideStatisticsDto getRideStatistics(String userId, boolean isDriver) {
        log.info("Getting ride statistics for user: {}, isDriver: {}", userId, isDriver);

        UUID userUUID = UUID.fromString(userId);
        List<RideRequest> rides;
        if (isDriver) {
            rides = rideRepository.findByDriverIdAndStatus(userUUID.toString(), RideStatus.COMPLETED.toString());
        } else {
            rides = rideRepository.findByCustomerIdAndStatus(userUUID.toString(), RideStatus.COMPLETED.toString());
        }

        int totalRides = rides.size();
        BigDecimal totalEarnings = rides.stream()
                .map(ride -> BigDecimal.valueOf(ride.getActualFare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageFare = totalRides > 0 ?
                totalEarnings.divide(new BigDecimal(totalRides), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate approximate distance (we don't have actual distance in RideRequest)
        long totalDistance = rides.size() * 5; // Assume average 5 miles per ride

        // Calculate duration between request and completion
        Duration totalDuration = rides.stream()
                .filter(ride -> ride.getRequestTime() != null && ride.getCompleteTime() != null)
                .map(ride -> Duration.between(ride.getRequestTime(), ride.getCompleteTime()))
                .reduce(Duration.ZERO, Duration::plus);

        return new RideStatisticsDto(
                totalRides,
                totalEarnings,
                averageFare,
                totalDistance,
                totalDuration.toHours(),
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );
    }

    /**
     * Export ride history as CSV
     */
    public String exportRideHistory(String userId, boolean isDriver, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Exporting ride history for user: {}, isDriver: {}, start: {}, end: {}",
                userId, isDriver, startDate, endDate);

        UUID userUUID = UUID.fromString(userId);
        List<RideRequest> allRides;
        if (isDriver) {
            allRides = rideRepository.findByDriverId(userUUID.toString());
        } else {
            allRides = rideRepository.findByCustomerId(userUUID.toString());
        }

        // Filter by date range
        List<RideRequest> rides = allRides.stream()
                .filter(ride -> ride.getRequestTime() != null)
                .filter(ride -> {
                    LocalDateTime requestTime = ride.getRequestTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    return requestTime.isAfter(startDate) && requestTime.isBefore(endDate);
                })
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Ride ID,Customer ID,Driver ID,Status,Requested At,Completed At,Fare,Distance\n");

        for (RideRequest ride : rides) {
            csv.append(ride.getRideId()).append(",")
                    .append(ride.getCustomerId()).append(",")
                    .append(ride.getDriverId()).append(",")
                    .append(ride.getStatus()).append(",")
                    .append(ride.getRequestTime()).append(",")
                    .append(ride.getCompleteTime()).append(",")
                    .append(ride.getActualFare()).append(",")
                    .append("N/A").append("\n"); // No distance field in RideRequest
        }

        return csv.toString();
    }

    private RideDto mapToRideDto(RideRequest ride) {
        return RideDto.builder()
                .rideId(ride.getRideId())
                .customerId(ride.getCustomerId() != null ? ride.getCustomerId().toString() : null)
                .pickupLatitude(BigDecimal.valueOf(ride.getPickupLatitude()))
                .pickupLongitude(BigDecimal.valueOf(ride.getPickupLongitude()))
                .dropoffLatitude(BigDecimal.valueOf(ride.getDropoffLatitude()))
                .dropoffLongitude(BigDecimal.valueOf(ride.getDropoffLongitude()))
                .requestedAt(ride.getRequestTime() != null ? ride.getRequestTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .completedAt(ride.getCompleteTime() != null ? ride.getCompleteTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .actualFare(BigDecimal.valueOf(ride.getActualFare()))
                .externalRideId(ride.getExternalRideId())
                .build();
    }

    private RideTrackingDto mapToTrackingDto(RideTracking tracking) {
        // Create LocationDto from coordinates
        LocationDto currentLocation = LocationDto.builder()
                .latitude(tracking.getLatitude())
                .longitude(tracking.getLongitude())
                .build();

        return RideTrackingDto.builder()
                .rideId(tracking.getRideId())
                .driverId(tracking.getDriverId())
                .currentLocation(currentLocation)
                .heading(tracking.getHeading())
                .speedMph(tracking.getSpeedMph())
                .accuracyMeters(tracking.getAccuracyMeters())
                .timestamp(tracking.getTimestamp())
                .build();
    }

    // DTO classes for the service
    public static class RideHistoryPage {
        private final List<RideDto> rides;
        private final long totalElements;
        private final int totalPages;
        private final int currentPage;
        private final int pageSize;
        private final boolean first;
        private final boolean last;

        public RideHistoryPage(List<RideDto> rides, long totalElements, int totalPages,
                             int currentPage, int pageSize, boolean first, boolean last) {
            this.rides = rides;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.first = first;
            this.last = last;
        }

        // Getters
        public List<RideDto> getRides() { return rides; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public int getCurrentPage() { return currentPage; }
        public int getPageSize() { return pageSize; }
        public boolean isFirst() { return first; }
        public boolean isLast() { return last; }
    }

    public static class RideDetailsDto {
        private final RideDto ride;
        private final List<RideTrackingDto> tracking;

        public RideDetailsDto(RideDto ride, List<RideTrackingDto> tracking) {
            this.ride = ride;
            this.tracking = tracking;
        }

        public RideDto getRide() { return ride; }
        public List<RideTrackingDto> getTracking() { return tracking; }
    }

    public static class RideReceiptDto {
        private final String rideId;
        private final String customerId;
        private final String driverId;
        private final LocalDateTime requestedAt;
        private final LocalDateTime completedAt;
        private final String pickupLocation;
        private final String dropoffLocation;
        private final BigDecimal baseFare;
        private final BigDecimal distanceFare;
        private final BigDecimal timeFare;
        private final BigDecimal tax;
        private final BigDecimal subtotal;
        private final BigDecimal total;
        private final String paymentMethod;
        private final String paymentInfo;

        public RideReceiptDto(String rideId, String customerId, String driverId,
                            LocalDateTime requestedAt, LocalDateTime completedAt,
                            String pickupLocation, String dropoffLocation,
                            BigDecimal baseFare, BigDecimal distanceFare, BigDecimal timeFare,
                            BigDecimal tax, BigDecimal subtotal, BigDecimal total,
                            String paymentMethod, String paymentInfo) {
            this.rideId = rideId;
            this.customerId = customerId;
            this.driverId = driverId;
            this.requestedAt = requestedAt;
            this.completedAt = completedAt;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.baseFare = baseFare;
            this.distanceFare = distanceFare;
            this.timeFare = timeFare;
            this.tax = tax;
            this.subtotal = subtotal;
            this.total = total;
            this.paymentMethod = paymentMethod;
            this.paymentInfo = paymentInfo;
        }

        // Getters
        public String getRideId() { return rideId; }
        public String getCustomerId() { return customerId; }
        public String getDriverId() { return driverId; }
        public LocalDateTime getRequestedAt() { return requestedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public String getPickupLocation() { return pickupLocation; }
        public String getDropoffLocation() { return dropoffLocation; }
        public BigDecimal getBaseFare() { return baseFare; }
        public BigDecimal getDistanceFare() { return distanceFare; }
        public BigDecimal getTimeFare() { return timeFare; }
        public BigDecimal getTax() { return tax; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getTotal() { return total; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getPaymentInfo() { return paymentInfo; }
    }

    public static class RideStatisticsDto {
        private final int totalRides;
        private final BigDecimal totalEarnings;
        private final BigDecimal averageFare;
        private final long totalDistance;
        private final long totalHours;
        private final LocalDateTime periodStart;
        private final LocalDateTime periodEnd;

        public RideStatisticsDto(int totalRides, BigDecimal totalEarnings, BigDecimal averageFare,
                               long totalDistance, long totalHours, LocalDateTime periodStart, LocalDateTime periodEnd) {
            this.totalRides = totalRides;
            this.totalEarnings = totalEarnings;
            this.averageFare = averageFare;
            this.totalDistance = totalDistance;
            this.totalHours = totalHours;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
        }

        // Getters
        public int getTotalRides() { return totalRides; }
        public BigDecimal getTotalEarnings() { return totalEarnings; }
        public BigDecimal getAverageFare() { return averageFare; }
        public long getTotalDistance() { return totalDistance; }
        public long getTotalHours() { return totalHours; }
        public LocalDateTime getPeriodStart() { return periodStart; }
        public LocalDateTime getPeriodEnd() { return periodEnd; }
    }
}