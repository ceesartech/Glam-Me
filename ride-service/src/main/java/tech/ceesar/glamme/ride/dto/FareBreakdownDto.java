package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for detailed fare breakdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareBreakdownDto {
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal timeFare;
    private BigDecimal bookingFee;
    private BigDecimal tolls;
    private BigDecimal tips;
    private BigDecimal taxes;
    private BigDecimal surgeMultiplier;
    private BigDecimal discount;
    private BigDecimal totalFare;
    private String currency;
    private String breakdownDescription;
}
