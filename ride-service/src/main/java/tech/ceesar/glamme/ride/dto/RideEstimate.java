package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for ride fare estimate from external providers
 */
@Data
@Builder
@NoArgsConstructor
public class RideEstimate {
    private String productId;
    private String displayName;
    private String estimate;
    private BigDecimal distance;
    private Integer duration;
    private BigDecimal surgeMultiplier;
    private String currency;

    // Explicit constructor for builder compatibility
    public RideEstimate(String productId, String displayName, String estimate,
                       BigDecimal distance, Integer duration, BigDecimal surgeMultiplier,
                       String currency) {
        this.productId = productId;
        this.displayName = displayName;
        this.estimate = estimate;
        this.distance = distance;
        this.duration = duration;
        this.surgeMultiplier = surgeMultiplier;
        this.currency = currency;
    }

    public static RideEstimateBuilder builder() {
        return new RideEstimateBuilder();
    }

    public static class RideEstimateBuilder {
        private String productId;
        private String displayName;
        private String estimate;
        private BigDecimal distance;
        private Integer duration;
        private BigDecimal surgeMultiplier;
        private String currency;

        public RideEstimateBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public RideEstimateBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public RideEstimateBuilder estimate(String estimate) {
            this.estimate = estimate;
            return this;
        }

        public RideEstimateBuilder distance(BigDecimal distance) {
            this.distance = distance;
            return this;
        }

        public RideEstimateBuilder duration(Integer duration) {
            this.duration = duration;
            return this;
        }

        public RideEstimateBuilder surgeMultiplier(BigDecimal surgeMultiplier) {
            this.surgeMultiplier = surgeMultiplier;
            return this;
        }

        public RideEstimateBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public RideEstimate build() {
            return new RideEstimate(productId, displayName, estimate, distance, duration, surgeMultiplier, currency);
        }
    }
}
