package tech.ceesar.glamme.matching.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStylistResuest {
    @NotNull
    private UUID userId;

    @NotNull
    private double latitude;

    @NotNull
    private double longitude;

    @NotNull
    private List<ServiceOfferingDto> offerings;
}
