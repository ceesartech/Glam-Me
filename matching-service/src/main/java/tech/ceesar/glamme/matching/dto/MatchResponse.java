package tech.ceesar.glamme.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.matching.entity.Match;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.time.LocalDateTime;

/**
 * Response DTO for match information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {

    private Long id;
    private String customerId;
    private String stylistId;

    private Double matchScore;
    private Match.MatchType matchType;
    private Match.Status status;
    private Match.Algorithm algorithm;

    private Stylist.Service requestedService;
    private LocalDateTime preferredDate;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;

    // Embedded stylist information for convenience
    private StylistResponse stylist;
    private String stylistBusinessName;
    private String stylistEmail;
    private Double stylistRating;
    private Boolean stylistIsVerified;
}
