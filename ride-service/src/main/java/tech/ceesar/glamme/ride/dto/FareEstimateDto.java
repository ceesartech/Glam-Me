package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for fare estimation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareEstimateDto {
    private String provider;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal timeFare;
    private BigDecimal surgeMultiplier;
    private BigDecimal totalFare;
    private String currency;
    private Integer estimatedDuration;
    private Double distance;
    private String vehicleType;
    private BigDecimal minimumFare;
    private BigDecimal maximumFare;
}
