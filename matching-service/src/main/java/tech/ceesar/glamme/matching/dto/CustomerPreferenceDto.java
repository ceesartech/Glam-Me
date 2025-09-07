package tech.ceesar.glamme.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.matching.entity.CustomerPreference;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerPreferenceDto {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotNull(message = "Preferred style is required")
    private CustomerPreference.Style preferredStyle;
    
    private BigDecimal budgetMin;
    
    private BigDecimal budgetMax;
    
    private Integer maxDistance; // in km
    
    private String preferredTimeSlots; // JSON string
    
    private CustomerPreference.ExperienceLevel experienceLevel;
    
    private String specialRequirements;
    
    private Boolean isActive;
}
