package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for ride fare estimation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideEstimateResponse {
    private String provider; // UBER, LYFT, INTERNAL
    private String productId; // Product identifier from provider
    private String displayName; // Human-readable product name
    private String estimate; // Formatted price estimate (e.g., "$15-20")
    private BigDecimal minPrice; // Minimum estimated price
    private BigDecimal maxPrice; // Maximum estimated price
    private Double distance; // Distance in miles/kilometers
    private Integer duration; // Duration in minutes
    private String currency; // Currency code (USD, EUR, etc.)
    private BigDecimal surgeMultiplier; // Surge pricing multiplier
    private Integer eta; // Estimated time of arrival in minutes
    private String vehicleType; // ECONOMY, PREMIUM, SUV, etc.
}
