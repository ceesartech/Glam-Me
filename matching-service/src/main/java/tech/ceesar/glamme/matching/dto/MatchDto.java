package tech.ceesar.glamme.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.matching.entity.Match;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchDto {
    
    private Long id;
    
    private String customerId;
    
    private String stylistId;
    
    private Double matchScore;
    
    private String matchReason;
    
    private Match.Status status;
    
    private Match.Algorithm algorithm;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime viewedAt;
    
    private LocalDateTime respondedAt;
    
    // Additional fields for display
    private StylistDto stylist;
    private CustomerPreferenceDto customerPreference;
}
