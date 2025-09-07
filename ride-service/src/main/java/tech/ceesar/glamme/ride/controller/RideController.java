package tech.ceesar.glamme.ride.controller;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.service.RideService;
import tech.ceesar.glamme.ride.service.RideTrackingService;
import tech.ceesar.glamme.ride.service.DriverTrackingService;
import tech.ceesar.glamme.ride.service.RideAnalyticsService;
import tech.ceesar.glamme.ride.service.RideHistoryService;
import tech.ceesar.glamme.ride.service.aws.RideProviderService;
import tech.ceesar.glamme.common.dto.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Slf4j
public class RideController {

    private final RideService rideService;
    private final RideTrackingService rideTrackingService;
    private final DriverTrackingService driverTrackingService;
    private final RideAnalyticsService rideAnalyticsService;
    private final RideHistoryService rideHistoryService;
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
                request.getPickupLocation(), request.getDropoffLocation())
                .map(estimate -> RideEstimateResponse.builder()
                        .provider("UBER")
                        .productId(estimate.getProductId())
                        .displayName(estimate.getDisplayName())
                        .estimate(estimate.getEstimate())
                        .distance(estimate.getDistance().doubleValue())
                        .duration(estimate.getDuration())
                        .build())
                .onErrorResume(error -> {
                    log.warn("Failed to get Uber estimate", error);
                    return reactor.core.publisher.Mono.empty();
                });

        var lyftEstimate = rideProviderService.getLyftEstimate(
                request.getPickupLocation(), request.getDropoffLocation())
                .map(estimate -> RideEstimateResponse.builder()
                        .provider("LYFT")
                        .productId(estimate.getProductId())
                        .displayName(estimate.getDisplayName())
                        .estimate(estimate.getEstimate())
                        .distance(estimate.getDistance().doubleValue())
                        .duration(estimate.getDuration())
                        .build())
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
    public reactor.core.publisher.Mono<ResponseEntity<tech.ceesar.glamme.ride.dto.RideEstimateResponse>> getRideEstimate(
            @PathVariable String provider,
            @RequestBody RideEstimateRequest request) {

        var estimate = switch (provider.toUpperCase()) {
            case "UBER" -> rideProviderService.getUberEstimate(
                    request.getPickupLocation(), request.getDropoffLocation());
            case "LYFT" -> rideProviderService.getLyftEstimate(
                    request.getPickupLocation(), request.getDropoffLocation());
            default -> reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Unsupported provider: " + provider));
        };

        return estimate
                .map(est -> {
                    RideEstimate estimateObj = (RideEstimate) est;
                    return tech.ceesar.glamme.ride.dto.RideEstimateResponse.builder()
                            .provider(provider.toUpperCase())
                            .productId(estimateObj.getProductId())
                            .displayName(estimateObj.getDisplayName())
                            .estimate(estimateObj.getEstimate())
                            .distance(estimateObj.getDistance() != null ? estimateObj.getDistance().doubleValue() : null)
                            .duration(estimateObj.getDuration())
                            .currency(estimateObj.getCurrency())
                            .surgeMultiplier(estimateObj.getSurgeMultiplier())
                            .build();
                })
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Failed to get {} estimate", provider, error);
                    return reactor.core.publisher.Mono.just(ResponseEntity.badRequest().build());
                });
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
                .map(det -> RideDetailsResponse.builder()
                        .rideId(det.getRideId())
                        .status(det.getStatus())
                        .driverName(det.getDriverName())
                        .driverPhone(det.getDriverPhone())
                        .vehicleModel(det.getVehicleModel())
                        .vehicleLicensePlate(det.getVehicleLicense())
                        .eta(det.getEta())
                        .build())
                .onErrorResume(error -> {
                    log.error("Failed to get ride details for {}", rideId, error);
                    return reactor.core.publisher.Mono.empty();
                });

        return details
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .block();
    }

    // ==================== RIDE TRACKING ENDPOINTS ====================

    /**
     * Update ride tracking with current location and progress
     */
    @PostMapping("/{rideId}/tracking")
    public ResponseEntity<ApiResponse<String>> updateRideTracking(
            @PathVariable UUID rideId,
            @RequestParam String driverId,
            @RequestBody LocationDto currentLocation,
            @RequestParam(required = false) BigDecimal distanceTraveled,
            @RequestParam(required = false) Integer etaToDestination) {

        try {
            rideTrackingService.updateRideProgress(
                    rideId.toString(),
                    driverId,
                    currentLocation,
                    distanceTraveled != null ? distanceTraveled : BigDecimal.ZERO,
                    etaToDestination
            );
            return ResponseEntity.ok(ApiResponse.success("Ride tracking updated successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to update ride tracking: " + e.getMessage()));
        }
    }

    /**
     * Get ride tracking history
     */
    @GetMapping("/{rideId}/tracking")
    public ResponseEntity<ApiResponse<List<RideTrackingDto>>> getRideTrackingHistory(
            @PathVariable UUID rideId) {

        try {
            List<RideTrackingDto> trackingHistory = rideTrackingService.getRideTrackingHistory(rideId.toString());
            return ResponseEntity.ok(ApiResponse.success(trackingHistory));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get ride tracking history: " + e.getMessage()));
        }
    }

    /**
     * Get latest ride tracking information
     */
    @GetMapping("/{rideId}/tracking/latest")
    public ResponseEntity<ApiResponse<RideTrackingDto>> getLatestRideTracking(
            @PathVariable UUID rideId) {

        try {
            Optional<RideTrackingDto> latestTracking = rideTrackingService.getLatestRideTracking(rideId.toString());
            if (latestTracking.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(latestTracking.get()));
            } else {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("No tracking data found for ride"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get latest ride tracking: " + e.getMessage()));
        }
    }

    // ==================== DRIVER TRACKING ENDPOINTS ====================

    /**
     * Update driver location
     */
    @PostMapping("/drivers/{driverId}/location")
    public ResponseEntity<ApiResponse<String>> updateDriverLocation(
            @PathVariable String driverId,
            @RequestBody LocationDto location,
            @RequestParam(required = false) Integer heading,
            @RequestParam(required = false) BigDecimal speed,
            @RequestParam(required = false) BigDecimal accuracy) {

        try {
            rideTrackingService.updateDriverLocation(
                    driverId,
                    location,
                    heading,
                    speed,
                    accuracy
            );
            return ResponseEntity.ok(ApiResponse.success("Driver location updated successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to update driver location: " + e.getMessage()));
        }
    }

    /**
     * Get driver current status and location
     */
    @GetMapping("/drivers/{driverId}/status")
    public ResponseEntity<ApiResponse<DriverTrackingDto>> getDriverStatus(
            @PathVariable String driverId) {

        try {
            Optional<DriverTrackingDto> driverStatus = driverTrackingService.getDriverStatus(driverId);
            if (driverStatus.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(driverStatus.get()));
            } else {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Driver not found"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get driver status: " + e.getMessage()));
        }
    }

    /**
     * Get driver's tracking history
     */
    @GetMapping("/drivers/{driverId}/tracking")
    public ResponseEntity<ApiResponse<List<RideTrackingDto>>> getDriverTrackingHistory(
            @PathVariable String driverId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        try {
            LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : LocalDateTime.now().minusDays(1);
            LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : LocalDateTime.now();

            List<RideTrackingDto> trackingHistory = rideTrackingService.getDriverTrackingHistory(driverId, start, end);
            return ResponseEntity.ok(ApiResponse.success(trackingHistory));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get driver tracking history: " + e.getMessage()));
        }
    }

    /**
     * Update driver status and availability
     */
    @PostMapping("/drivers/{driverId}/status")
    public ResponseEntity<ApiResponse<String>> updateDriverStatus(
            @PathVariable String driverId,
            @RequestParam String status,
            @RequestParam Boolean available,
            @RequestBody(required = false) LocationDto location) {

        try {
            driverTrackingService.updateDriverStatus(driverId, status, available, location);
            return ResponseEntity.ok(ApiResponse.success("Driver status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to update driver status: " + e.getMessage()));
        }
    }

    /**
     * Start driver shift
     */
    @PostMapping("/drivers/{driverId}/shift/start")
    public ResponseEntity<ApiResponse<String>> startDriverShift(
            @PathVariable String driverId,
            @RequestParam String vehicleId) {

        try {
            driverTrackingService.startDriverShift(driverId, vehicleId);
            return ResponseEntity.ok(ApiResponse.success("Driver shift started successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to start driver shift: " + e.getMessage()));
        }
    }

    /**
     * End driver shift
     */
    @PostMapping("/drivers/{driverId}/shift/end")
    public ResponseEntity<ApiResponse<String>> endDriverShift(
            @PathVariable String driverId) {

        try {
            driverTrackingService.endDriverShift(driverId);
            return ResponseEntity.ok(ApiResponse.success("Driver shift ended successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to end driver shift: " + e.getMessage()));
        }
    }

    /**
     * Get available drivers in an area
     */
    @GetMapping("/drivers/available")
    public ResponseEntity<ApiResponse<List<DriverTrackingDto>>> getAvailableDrivers(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm) {

        try {
            List<DriverTrackingDto> availableDrivers = driverTrackingService.getAvailableDriversInArea(
                    latitude, longitude, radiusKm);
            return ResponseEntity.ok(ApiResponse.success(availableDrivers));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get available drivers: " + e.getMessage()));
        }
    }

    /**
     * Get driver performance metrics
     */
    @GetMapping("/drivers/{driverId}/performance")
    public ResponseEntity<ApiResponse<DriverTrackingDto>> getDriverPerformance(
            @PathVariable String driverId) {

        try {
            DriverTrackingDto performance = driverTrackingService.getDriverPerformanceMetrics(driverId);
            if (performance != null) {
                return ResponseEntity.ok(ApiResponse.success(performance));
            } else {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Driver not found"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get driver performance: " + e.getMessage()));
        }
    }

    // ==================== RIDE MANAGEMENT ENDPOINTS ====================

    /**
     * Update ride information
     */
    @PutMapping("/{rideId}")
    public ResponseEntity<ApiResponse<String>> updateRide(
            @PathVariable UUID rideId,
            @RequestBody RideUpdateRequest updateRequest) {

        try {
            // Implementation would update ride details
            return ResponseEntity.ok(ApiResponse.success("Ride updated successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to update ride: " + e.getMessage()));
        }
    }

    /**
     * Get ride history for a customer
     */
    @GetMapping("/customer/{customerId}/history")
    public ResponseEntity<ApiResponse<RideHistoryService.RideHistoryPage>> getCustomerRideHistory(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            RideHistoryService.RideHistoryPage history = rideHistoryService.getCustomerRideHistory(
                    customerId, page, size, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get customer ride history: " + e.getMessage()));
        }
    }

    /**
     * Get ride history for a driver
     */
    @GetMapping("/driver/{driverId}/history")
    public ResponseEntity<ApiResponse<RideHistoryService.RideHistoryPage>> getDriverRideHistory(
            @PathVariable String driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            RideHistoryService.RideHistoryPage history = rideHistoryService.getDriverRideHistory(
                    driverId, page, size, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get driver ride history: " + e.getMessage()));
        }
    }

    /**
     * Get detailed ride information
     */
    @GetMapping("/{rideId}/details")
    public ResponseEntity<ApiResponse<RideHistoryService.RideDetailsDto>> getRideDetails(
            @PathVariable String rideId) {

        try {
            Optional<RideHistoryService.RideDetailsDto> details = rideHistoryService.getRideDetails(rideId);
            if (details.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(details.get()));
            } else {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Ride not found"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get ride details: " + e.getMessage()));
        }
    }

    /**
     * Get ride receipt
     */
    @GetMapping("/{rideId}/receipt")
    public ResponseEntity<ApiResponse<RideHistoryService.RideReceiptDto>> getRideReceipt(
            @PathVariable String rideId) {

        try {
            Optional<RideHistoryService.RideReceiptDto> receipt = rideHistoryService.getRideReceipt(rideId);
            if (receipt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(receipt.get()));
            } else {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Receipt not available for this ride"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get ride receipt: " + e.getMessage()));
        }
    }

    /**
     * Get user ride statistics
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ApiResponse<RideHistoryService.RideStatisticsDto>> getUserRideStatistics(
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean isDriver) {

        try {
            RideHistoryService.RideStatisticsDto stats = rideHistoryService.getRideStatistics(userId, isDriver);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get user ride statistics: " + e.getMessage()));
        }
    }

    /**
     * Export ride history
     */
    @GetMapping("/export/{userId}")
    public ResponseEntity<String> exportRideHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "false") boolean isDriver,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();

            String csvData = rideHistoryService.exportRideHistory(userId, isDriver, start, end);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=ride_history_" + userId + ".csv")
                    .body(csvData);

        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body("Error generating export: " + e.getMessage());
        }
    }

    /**
     * Calculate distance and ETA between two points
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateRideMetrics(
            @RequestBody LocationDto pickup,
            @RequestBody LocationDto dropoff) {

        try {
            double distance = rideTrackingService.calculateDistance(
                    pickup.getLatitude(),
                    pickup.getLongitude(),
                    dropoff.getLatitude(),
                    dropoff.getLongitude()
            );

            Integer eta = rideTrackingService.estimateETA(pickup, dropoff, 30.0); // 30 mph average

            Map<String, Object> metrics = Map.of(
                    "distanceMiles", distance,
                    "estimatedTimeMinutes", eta,
                    "pickup", pickup,
                    "dropoff", dropoff
            );

            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to calculate ride metrics: " + e.getMessage()));
        }
    }

    // ==================== ANALYTICS ENDPOINTS ====================

    /**
     * Get comprehensive ride analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<RideAnalyticsDto>> getRideAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();

            RideAnalyticsDto analytics = rideAnalyticsService.getRideAnalytics(start, end);
            return ResponseEntity.ok(ApiResponse.success(analytics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get ride analytics: " + e.getMessage()));
        }
    }

    /**
     * Get driver performance analytics
     */
    @GetMapping("/analytics/drivers/{driverId}")
    public ResponseEntity<ApiResponse<RideAnalyticsService.DriverAnalyticsDto>> getDriverAnalytics(
            @PathVariable String driverId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();

            RideAnalyticsService.DriverAnalyticsDto analytics = rideAnalyticsService.getDriverAnalytics(driverId, start, end);
            return ResponseEntity.ok(ApiResponse.success(analytics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get driver analytics: " + e.getMessage()));
        }
    }

    /**
     * Get revenue analytics
     */
    @GetMapping("/analytics/revenue")
    public ResponseEntity<ApiResponse<RideAnalyticsService.RevenueAnalyticsDto>> getRevenueAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();

            RideAnalyticsService.RevenueAnalyticsDto analytics = rideAnalyticsService.getRevenueAnalytics(start, end);
            return ResponseEntity.ok(ApiResponse.success(analytics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get revenue analytics: " + e.getMessage()));
        }
    }

    /**
     * Get operational metrics
     */
    @GetMapping("/analytics/operational")
    public ResponseEntity<ApiResponse<RideAnalyticsService.OperationalMetricsDto>> getOperationalMetrics() {

        try {
            RideAnalyticsService.OperationalMetricsDto metrics = rideAnalyticsService.getOperationalMetrics();
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get operational metrics: " + e.getMessage()));
        }
    }

    /**
     * Get geographical analytics
     */
    @GetMapping("/analytics/geographical")
    public ResponseEntity<ApiResponse<RideAnalyticsService.GeographicalAnalyticsDto>> getGeographicalAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();

            RideAnalyticsService.GeographicalAnalyticsDto analytics = rideAnalyticsService.getGeographicalAnalytics(start, end);
            return ResponseEntity.ok(ApiResponse.success(analytics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to get geographical analytics: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint with comprehensive status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthStatus = Map.of(
                "service", "ride-service",
                "status", "operational",
                "features", List.of(
                        "ride booking and management",
                        "real-time driver tracking",
                        "ride progress monitoring",
                        "payment processing",
                        "driver shift management",
                        "location-based services",
                        "WebSocket notifications",
                        "external provider integration",
                        "comprehensive analytics",
                        "business intelligence"
                ),
                "timestamp", LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.success(healthStatus,
                "Ride service is fully operational with comprehensive tracking, monitoring, and analytics capabilities"));
    }

    // Record definitions removed - using DTOs instead
}
