package tech.ceesar.glamme.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchingRequest {
    
    @NotNull(message = "Customer ID is required")
    private String customerId;
    
    private BigDecimal customerLatitude;
    
    private BigDecimal customerLongitude;
    
    private String preferredStyle;
    
    private BigDecimal budgetMin;
    
    private BigDecimal budgetMax;
    
    private Integer maxDistance; // in km
    
    private List<String> preferredSpecialties;
    
    private List<String> preferredServices;
    
    private Integer limit; // max number of matches to return
    
    private String algorithm; // ELO, GALE_SHAPLEY, HYBRID
    
    private Boolean includeUnavailable; // include stylists who are currently unavailable
}
