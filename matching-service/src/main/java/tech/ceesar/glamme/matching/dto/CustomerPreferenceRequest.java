package tech.ceesar.glamme.matching.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import tech.ceesar.glamme.matching.entity.CustomerPreference;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Request DTO for updating customer preferences
 */
@Data
public class CustomerPreferenceRequest {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer maxDistanceKm;

    private Set<Stylist.Specialty> preferredSpecialties;
    private Set<Stylist.Service> preferredServices;

    private BigDecimal priceRangeMin;
    private BigDecimal priceRangeMax;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal minRating;

    private Boolean preferVerified;
    private Boolean preferExperienced;
    private Integer minYearsExperience;

    private Set<String> preferredLanguages;
    private String availabilityPreferences; // JSON string
}
