package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Analytics DTO for ride statistics and reporting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideAnalyticsDto {
    // Overall Statistics
    private Long totalRides;
    private Long completedRides;
    private Long cancelledRides;
    private BigDecimal completionRate; // Percentage

    // Financial Metrics
    private BigDecimal totalRevenue;
    private BigDecimal averageFare;
    private BigDecimal totalDriverEarnings;
    private BigDecimal platformFee; // Platform's share

    // Performance Metrics
    private Double averageRideDuration; // minutes
    private Double averageDistance; // miles/km
    private Double averageRating; // 1-5 scale

    // Time-based Metrics
    private Long ridesToday;
    private Long ridesThisWeek;
    private Long ridesThisMonth;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;

    // Provider Metrics
    private Long uberRides;
    private Long lyftRides;
    private Long internalRides;
    private BigDecimal uberRevenue;
    private BigDecimal lyftRevenue;
    private BigDecimal internalRevenue;

    // Driver Metrics
    private Long activeDrivers;
    private Long availableDrivers;
    private Double averageDriverRating;
    private Long topPerformingDrivers;

    // Customer Metrics
    private Long uniqueCustomers;
    private Long repeatCustomers;
    private Double averageCustomerRating;
}
