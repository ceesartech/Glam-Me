package tech.ceesar.glamme.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceOfferingDto {
    private UUID id;
    private String styleName;
    private double costPerHour;
    private double estimatedHours;
    private List<AddOnDto> addOns;

    /**
     * Computed total cost = (rate × hours) + sum(addOn costs)
     */
    public double getTotalCost() {
        return costPerHour * estimatedHours +
                addOns.stream().mapToDouble(AddOnDto::getCost).sum();
    }
}
