package tech.ceesar.glamme.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.ride.dto.FareEstimateDto;
import tech.ceesar.glamme.ride.dto.LocationDto;
import tech.ceesar.glamme.ride.dto.FareBreakdownDto;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive fare estimation service with dynamic pricing capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FareEstimationService {

    private final DriverProfileRepository driverRepository;

    // Base fare rates (configurable)
    private static final BigDecimal BASE_FARE = BigDecimal.valueOf(2.50);
    private static final BigDecimal PER_MILE_RATE = BigDecimal.valueOf(1.25);
    private static final BigDecimal PER_MINUTE_RATE = BigDecimal.valueOf(0.35);
    private static final BigDecimal MINIMUM_FARE = BigDecimal.valueOf(5.00);
    private static final BigDecimal CANCELLATION_FEE = BigDecimal.valueOf(5.00);

    // Surge pricing multipliers
    private static final BigDecimal PEAK_HOURS_MULTIPLIER = BigDecimal.valueOf(1.5);
    private static final BigDecimal HIGH_DEMAND_MULTIPLIER = BigDecimal.valueOf(2.0);
    private static final BigDecimal WEATHER_MULTIPLIER = BigDecimal.valueOf(1.3);
    private static final BigDecimal EVENT_MULTIPLIER = BigDecimal.valueOf(1.8);

    // Service fees
    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = BigDecimal.valueOf(0.20); // 20%
    private static final BigDecimal DRIVER_EARNINGS_PERCENTAGE = BigDecimal.valueOf(0.80); // 80%

    // Cache for dynamic pricing data
    private final Map<String, SurgeData> surgePricingCache = new ConcurrentHashMap<>();

    /**
     * Estimate fare for a ride
     */
    @Cacheable(value = "fareEstimates", key = "#pickup.hashCode() + '_' + #dropoff.hashCode() + '_' + T(java.time.LocalDateTime).now().toLocalDate()")
    public FareEstimateDto estimateFare(LocationDto pickup, LocationDto dropoff,
                                      String vehicleType, Integer passengerCount, boolean sharedRide) {
        log.info("Estimating fare for ride from {} to {}", pickup, dropoff);

        try {
            // Calculate distance and time
            double distance = calculateDistance(pickup, dropoff);
            double estimatedTime = estimateTravelTime(distance, vehicleType);

            // Calculate base fare
            BigDecimal baseFare = calculateBaseFare(distance, estimatedTime, vehicleType);

            // Apply surge pricing
            BigDecimal surgeMultiplier = calculateSurgeMultiplier(pickup, LocalDateTime.now());
            BigDecimal surgeFare = baseFare.multiply(surgeMultiplier);

            // Apply additional fees
            BigDecimal bookingFee = BigDecimal.valueOf(1.00);
            BigDecimal serviceFee = surgeFare.multiply(PLATFORM_FEE_PERCENTAGE);
            BigDecimal taxes = surgeFare.multiply(BigDecimal.valueOf(0.085)); // 8.5% tax

            // Calculate total
            BigDecimal subtotal = surgeFare.add(bookingFee);
            BigDecimal total = subtotal.add(serviceFee).add(taxes);

            // Ensure minimum fare
            if (total.compareTo(MINIMUM_FARE) < 0) {
                total = MINIMUM_FARE;
                subtotal = MINIMUM_FARE.subtract(serviceFee).subtract(taxes);
            }

            // Create fare breakdown
            FareBreakdownDto breakdown = FareBreakdownDto.builder()
                    .baseFare(baseFare)
                    .distanceFare(PER_MILE_RATE.multiply(BigDecimal.valueOf(distance)))
                    .timeFare(PER_MINUTE_RATE.multiply(BigDecimal.valueOf(estimatedTime)))
                    .surgeFare(surgeFare.subtract(baseFare))
                    .bookingFee(bookingFee)
                    .serviceFee(serviceFee)
                    .taxes(taxes)
                    .total(total)
                    .build();

            return FareEstimateDto.builder()
                    .pickupLocation(pickup)
                    .dropoffLocation(dropoff)
                    .distanceMiles(BigDecimal.valueOf(distance))
                    .estimatedDurationMinutes((int) Math.round(estimatedTime))
                    .vehicleType(vehicleType)
                    .passengerCount(passengerCount)
                    .sharedRide(sharedRide)
                    .surgeMultiplier(surgeMultiplier)
                    .currency("USD")
                    .breakdown(breakdown)
                    .estimatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error estimating fare: {}", e.getMessage());
            // Return a default estimate
            return createDefaultFareEstimate(pickup, dropoff, vehicleType);
        }
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    public double calculateDistance(LocationDto point1, LocationDto point2) {
        final int R = 6371; // Radius of the earth in km

        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance * 0.621371; // Convert to miles
    }

    /**
     * Estimate travel time based on distance and vehicle type
     */
    private double estimateTravelTime(double distance, String vehicleType) {
        double averageSpeedMph;

        switch (vehicleType != null ? vehicleType.toUpperCase() : "ECONOMY") {
            case "LUXURY":
            case "PREMIUM":
                averageSpeedMph = 35.0; // Slower due to traffic, but premium routes
                break;
            case "SUV":
                averageSpeedMph = 32.0;
                break;
            case "ECONOMY":
            default:
                averageSpeedMph = 28.0; // Standard city driving speed
                break;
        }

        // Add time for pickup/dropoff (3 minutes each)
        double travelTime = (distance / averageSpeedMph) * 60; // Convert to minutes
        return travelTime + 6; // 6 minutes for pickup/dropoff
    }

    /**
     * Calculate base fare
     */
    private BigDecimal calculateBaseFare(double distance, double timeMinutes, String vehicleType) {
        BigDecimal distanceFare = PER_MILE_RATE.multiply(BigDecimal.valueOf(distance));
        BigDecimal timeFare = PER_MINUTE_RATE.multiply(BigDecimal.valueOf(timeMinutes));

        BigDecimal totalFare = BASE_FARE.add(distanceFare).add(timeFare);

        // Apply vehicle type multiplier
        BigDecimal vehicleMultiplier = getVehicleTypeMultiplier(vehicleType);
        totalFare = totalFare.multiply(vehicleMultiplier);

        return totalFare.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get vehicle type fare multiplier
     */
    private BigDecimal getVehicleTypeMultiplier(String vehicleType) {
        if (vehicleType == null) return BigDecimal.ONE;

        switch (vehicleType.toUpperCase()) {
            case "LUXURY":
            case "PREMIUM":
                return BigDecimal.valueOf(1.5);
            case "SUV":
                return BigDecimal.valueOf(1.3);
            case "ECONOMY":
            default:
                return BigDecimal.ONE;
        }
    }

    /**
     * Calculate surge pricing multiplier
     */
    private BigDecimal calculateSurgeMultiplier(LocationDto location, LocalDateTime dateTime) {
        BigDecimal multiplier = BigDecimal.ONE;

        // Time-based surge
        multiplier = multiplier.multiply(calculateTimeBasedSurge(dateTime));

        // Demand-based surge (simplified - would integrate with real-time demand data)
        multiplier = multiplier.multiply(calculateDemandBasedSurge(location, dateTime));

        // Weather-based surge (simplified - would integrate with weather API)
        multiplier = multiplier.multiply(calculateWeatherBasedSurge(location, dateTime));

        // Event-based surge (simplified - would integrate with event data)
        multiplier = multiplier.multiply(calculateEventBasedSurge(location, dateTime));

        // Ensure reasonable bounds
        if (multiplier.compareTo(BigDecimal.valueOf(3.0)) > 0) {
            multiplier = BigDecimal.valueOf(3.0); // Max 3x surge
        }

        return multiplier.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate time-based surge pricing
     */
    private BigDecimal calculateTimeBasedSurge(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        int hour = time.getHour();

        // Peak hours: 7-9 AM and 4-7 PM on weekdays
        boolean isWeekday = dateTime.getDayOfWeek().getValue() < 6;
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19);

        if (isWeekday && isPeakHour) {
            return PEAK_HOURS_MULTIPLIER;
        }

        // Late night hours: 10 PM - 6 AM
        if (hour >= 22 || hour <= 6) {
            return BigDecimal.valueOf(1.2);
        }

        return BigDecimal.ONE;
    }

    /**
     * Calculate demand-based surge pricing
     */
    private BigDecimal calculateDemandBasedSurge(LocationDto location, LocalDateTime dateTime) {
        // Simplified demand calculation - would integrate with real-time data
        String cacheKey = location.getLatitude() + "," + location.getLongitude() + "_" + dateTime.toLocalDate();

        SurgeData surgeData = surgePricingCache.computeIfAbsent(cacheKey, k -> {
            // Simulate demand calculation
            double randomDemand = Math.random();
            if (randomDemand > 0.8) {
                return new SurgeData(HIGH_DEMAND_MULTIPLIER, dateTime.plusMinutes(30));
            } else if (randomDemand > 0.6) {
                return new SurgeData(BigDecimal.valueOf(1.3), dateTime.plusMinutes(30));
            } else {
                return new SurgeData(BigDecimal.ONE, dateTime.plusMinutes(30));
            }
        });

        // Check if surge data is still valid
        if (dateTime.isAfter(surgeData.expiryTime)) {
            surgePricingCache.remove(cacheKey);
            return BigDecimal.ONE;
        }

        return surgeData.multiplier;
    }

    /**
     * Calculate weather-based surge pricing
     */
    private BigDecimal calculateWeatherBasedSurge(LocationDto location, LocalDateTime dateTime) {
        // Simplified weather surge - would integrate with weather API
        // For demo purposes, add surge on "rainy" days
        if (dateTime.getDayOfMonth() % 3 == 0) { // Every 3rd day is "rainy"
            return WEATHER_MULTIPLIER;
        }
        return BigDecimal.ONE;
    }

    /**
     * Calculate event-based surge pricing
     */
    private BigDecimal calculateEventBasedSurge(LocationDto location, LocalDateTime dateTime) {
        // Simplified event surge - would integrate with event calendar API
        // For demo purposes, add surge on "event" days
        if (dateTime.getDayOfMonth() % 7 == 0) { // Every 7th day has "events"
            return EVENT_MULTIPLIER;
        }
        return BigDecimal.ONE;
    }

    /**
     * Create default fare estimate for error cases
     */
    private FareEstimateDto createDefaultFareEstimate(LocationDto pickup, LocationDto dropoff, String vehicleType) {
        FareBreakdownDto breakdown = FareBreakdownDto.builder()
                .baseFare(BASE_FARE)
                .distanceFare(BigDecimal.valueOf(5.00))
                .timeFare(BigDecimal.valueOf(3.50))
                .surgeFare(BigDecimal.ZERO)
                .bookingFee(BigDecimal.ONE)
                .serviceFee(BigDecimal.valueOf(2.00))
                .taxes(BigDecimal.valueOf(0.50))
                .total(BigDecimal.valueOf(15.00))
                .build();

        return FareEstimateDto.builder()
                .pickupLocation(pickup)
                .dropoffLocation(dropoff)
                .distanceMiles(BigDecimal.valueOf(3.0))
                .estimatedDurationMinutes(15)
                .vehicleType(vehicleType != null ? vehicleType : "ECONOMY")
                .passengerCount(1)
                .sharedRide(false)
                .surgeMultiplier(BigDecimal.ONE)
                .currency("USD")
                .breakdown(breakdown)
                .estimatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Calculate cancellation fee
     */
    public BigDecimal calculateCancellationFee(String rideId, LocalDateTime cancellationTime, LocalDateTime pickupTime) {
        // Free cancellation up to 2 minutes before pickup
        if (pickupTime != null && cancellationTime.isBefore(pickupTime.minusMinutes(2))) {
            return BigDecimal.ZERO;
        }

        // Full cancellation fee if within 2 minutes of pickup
        return CANCELLATION_FEE;
    }

    /**
     * Get fare estimate comparison across providers
     */
    public List<FareEstimateDto> getFareComparison(LocationDto pickup, LocationDto dropoff, String vehicleType) {
        // This would compare fares across Uber, Lyft, and internal pricing
        // For now, return our internal estimate
        return List.of(estimateFare(pickup, dropoff, vehicleType, 1, false));
    }

    /**
     * Update surge pricing data (would be called by external systems)
     */
    public void updateSurgeData(String locationKey, BigDecimal multiplier, LocalDateTime expiryTime) {
        surgePricingCache.put(locationKey, new SurgeData(multiplier, expiryTime));
        log.info("Updated surge pricing for {}: {}x until {}", locationKey, multiplier, expiryTime);
    }

    /**
     * Clear expired surge pricing data
     */
    public void cleanupExpiredSurgeData() {
        LocalDateTime now = LocalDateTime.now();
        surgePricingCache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));
        log.info("Cleaned up expired surge pricing data");
    }

    // Inner classes

    private static class SurgeData {
        private final BigDecimal multiplier;
        private final LocalDateTime expiryTime;

        public SurgeData(BigDecimal multiplier, LocalDateTime expiryTime) {
            this.multiplier = multiplier;
            this.expiryTime = expiryTime;
        }
    }

    // DTO classes for fare estimation

    public static class FareEstimateDto {
        private LocationDto pickupLocation;
        private LocationDto dropoffLocation;
        private BigDecimal distanceMiles;
        private Integer estimatedDurationMinutes;
        private String vehicleType;
        private Integer passengerCount;
        private Boolean sharedRide;
        private BigDecimal surgeMultiplier;
        private String currency;
        private FareBreakdownDto breakdown;
        private LocalDateTime estimatedAt;

        public static FareEstimateDtoBuilder builder() {
            return new FareEstimateDtoBuilder();
        }

        public static class FareEstimateDtoBuilder {
            private LocationDto pickupLocation;
            private LocationDto dropoffLocation;
            private BigDecimal distanceMiles;
            private Integer estimatedDurationMinutes;
            private String vehicleType;
            private Integer passengerCount;
            private Boolean sharedRide;
            private BigDecimal surgeMultiplier;
            private String currency;
            private FareBreakdownDto breakdown;
            private LocalDateTime estimatedAt;

            public FareEstimateDtoBuilder pickupLocation(LocationDto pickupLocation) {
                this.pickupLocation = pickupLocation;
                return this;
            }

            public FareEstimateDtoBuilder dropoffLocation(LocationDto dropoffLocation) {
                this.dropoffLocation = dropoffLocation;
                return this;
            }

            public FareEstimateDtoBuilder distanceMiles(BigDecimal distanceMiles) {
                this.distanceMiles = distanceMiles;
                return this;
            }

            public FareEstimateDtoBuilder estimatedDurationMinutes(Integer estimatedDurationMinutes) {
                this.estimatedDurationMinutes = estimatedDurationMinutes;
                return this;
            }

            public FareEstimateDtoBuilder vehicleType(String vehicleType) {
                this.vehicleType = vehicleType;
                return this;
            }

            public FareEstimateDtoBuilder passengerCount(Integer passengerCount) {
                this.passengerCount = passengerCount;
                return this;
            }

            public FareEstimateDtoBuilder sharedRide(Boolean sharedRide) {
                this.sharedRide = sharedRide;
                return this;
            }

            public FareEstimateDtoBuilder surgeMultiplier(BigDecimal surgeMultiplier) {
                this.surgeMultiplier = surgeMultiplier;
                return this;
            }

            public FareEstimateDtoBuilder currency(String currency) {
                this.currency = currency;
                return this;
            }

            public FareEstimateDtoBuilder breakdown(FareBreakdownDto breakdown) {
                this.breakdown = breakdown;
                return this;
            }

            public FareEstimateDtoBuilder estimatedAt(LocalDateTime estimatedAt) {
                this.estimatedAt = estimatedAt;
                return this;
            }

            public FareEstimateDto build() {
                FareEstimateDto dto = new FareEstimateDto();
                dto.pickupLocation = this.pickupLocation;
                dto.dropoffLocation = this.dropoffLocation;
                dto.distanceMiles = this.distanceMiles;
                dto.estimatedDurationMinutes = this.estimatedDurationMinutes;
                dto.vehicleType = this.vehicleType;
                dto.passengerCount = this.passengerCount;
                dto.sharedRide = this.sharedRide;
                dto.surgeMultiplier = this.surgeMultiplier;
                dto.currency = this.currency;
                dto.breakdown = this.breakdown;
                dto.estimatedAt = this.estimatedAt;
                return dto;
            }
        }

        // Getters and setters
        public LocationDto getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(LocationDto pickupLocation) { this.pickupLocation = pickupLocation; }

        public LocationDto getDropoffLocation() { return dropoffLocation; }
        public void setDropoffLocation(LocationDto dropoffLocation) { this.dropoffLocation = dropoffLocation; }

        public BigDecimal getDistanceMiles() { return distanceMiles; }
        public void setDistanceMiles(BigDecimal distanceMiles) { this.distanceMiles = distanceMiles; }

        public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
        public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

        public Integer getPassengerCount() { return passengerCount; }
        public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }

        public Boolean getSharedRide() { return sharedRide; }
        public void setSharedRide(Boolean sharedRide) { this.sharedRide = sharedRide; }

        public BigDecimal getSurgeMultiplier() { return surgeMultiplier; }
        public void setSurgeMultiplier(BigDecimal surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public FareBreakdownDto getBreakdown() { return breakdown; }
        public void setBreakdown(FareBreakdownDto breakdown) { this.breakdown = breakdown; }

        public LocalDateTime getEstimatedAt() { return estimatedAt; }
        public void setEstimatedAt(LocalDateTime estimatedAt) { this.estimatedAt = estimatedAt; }
    }

    public static class FareBreakdownDto {
        private BigDecimal baseFare;
        private BigDecimal distanceFare;
        private BigDecimal timeFare;
        private BigDecimal surgeFare;
        private BigDecimal bookingFee;
        private BigDecimal serviceFee;
        private BigDecimal taxes;
        private BigDecimal total;

        public static FareBreakdownDtoBuilder builder() {
            return new FareBreakdownDtoBuilder();
        }

        public static class FareBreakdownDtoBuilder {
            private BigDecimal baseFare;
            private BigDecimal distanceFare;
            private BigDecimal timeFare;
            private BigDecimal surgeFare;
            private BigDecimal bookingFee;
            private BigDecimal serviceFee;
            private BigDecimal taxes;
            private BigDecimal total;

            public FareBreakdownDtoBuilder baseFare(BigDecimal baseFare) {
                this.baseFare = baseFare;
                return this;
            }

            public FareBreakdownDtoBuilder distanceFare(BigDecimal distanceFare) {
                this.distanceFare = distanceFare;
                return this;
            }

            public FareBreakdownDtoBuilder timeFare(BigDecimal timeFare) {
                this.timeFare = timeFare;
                return this;
            }

            public FareBreakdownDtoBuilder surgeFare(BigDecimal surgeFare) {
                this.surgeFare = surgeFare;
                return this;
            }

            public FareBreakdownDtoBuilder bookingFee(BigDecimal bookingFee) {
                this.bookingFee = bookingFee;
                return this;
            }

            public FareBreakdownDtoBuilder serviceFee(BigDecimal serviceFee) {
                this.serviceFee = serviceFee;
                return this;
            }

            public FareBreakdownDtoBuilder taxes(BigDecimal taxes) {
                this.taxes = taxes;
                return this;
            }

            public FareBreakdownDtoBuilder total(BigDecimal total) {
                this.total = total;
                return this;
            }

            public FareBreakdownDto build() {
                FareBreakdownDto dto = new FareBreakdownDto();
                dto.baseFare = this.baseFare;
                dto.distanceFare = this.distanceFare;
                dto.timeFare = this.timeFare;
                dto.surgeFare = this.surgeFare;
                dto.bookingFee = this.bookingFee;
                dto.serviceFee = this.serviceFee;
                dto.taxes = this.taxes;
                dto.total = this.total;
                return dto;
            }
        }

        // Getters and setters
        public BigDecimal getBaseFare() { return baseFare; }
        public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

        public BigDecimal getDistanceFare() { return distanceFare; }
        public void setDistanceFare(BigDecimal distanceFare) { this.distanceFare = distanceFare; }

        public BigDecimal getTimeFare() { return timeFare; }
        public void setTimeFare(BigDecimal timeFare) { this.timeFare = timeFare; }

        public BigDecimal getSurgeFare() { return surgeFare; }
        public void setSurgeFare(BigDecimal surgeFare) { this.surgeFare = surgeFare; }

        public BigDecimal getBookingFee() { return bookingFee; }
        public void setBookingFee(BigDecimal bookingFee) { this.bookingFee = bookingFee; }

        public BigDecimal getServiceFee() { return serviceFee; }
        public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }

        public BigDecimal getTaxes() { return taxes; }
        public void setTaxes(BigDecimal taxes) { this.taxes = taxes; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
    }
}
