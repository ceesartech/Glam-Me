package tech.ceesar.glamme.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.ceesar.glamme.matching.entity.Match;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a match
 */
@Data
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
