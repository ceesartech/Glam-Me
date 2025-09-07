package tech.ceesar.glamme.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.matching.entity.Match;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {

    @NotBlank
    private String customerId;

    @NotBlank
    private String stylistId;

    @NotNull
    private Stylist.Service requestedService;

    private LocalDateTime preferredDate;
    private String notes;
    private Match.Algorithm algorithm;
}
