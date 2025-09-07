package tech.ceesar.glamme.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Request DTO for stylist onboarding
 */
@Data
public class StylistOnboardingRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    private String description;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    private String address;
    private String city;
    private String state;
    private String zipCode;

    private String phoneNumber;
    private String email;
    private String website;
    private String instagramHandle;

    private String profileImageUrl;
    private String[] portfolioImages;

    private Set<Stylist.Specialty> specialties;
    private Set<Stylist.Service> services;

    private BigDecimal priceRangeMin;
    private BigDecimal priceRangeMax;

    private Integer yearsExperience;
    private String[] certifications;
    private String[] languages;
}
