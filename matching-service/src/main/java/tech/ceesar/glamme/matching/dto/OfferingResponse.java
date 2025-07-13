package tech.ceesar.glamme.matching.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class OfferingResponse {
    private UUID offeringId;
    private UUID stylistProfileId;
    private UUID userId;
    private Set<String> specialties;
    private double eloRating;
    private double distance;      // in km
    private String styleName;
    private double costPerHour;
    private double estimatedHours;
    private List<AddOnDto> addOns;

    /**
     * Computed total cost = (rate × hours) + sum(addOn costs)
     */
    public double getTotalCost() {
        double addonsSum = addOns.stream()
                .mapToDouble(AddOnDto::getCost)
                .sum();
        return costPerHour * estimatedHours + addonsSum;
    }
}
