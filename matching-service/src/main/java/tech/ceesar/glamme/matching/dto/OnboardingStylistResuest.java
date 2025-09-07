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

    private String businessName;
    private String description;

    @NotNull
    private double latitude;

    @NotNull
    private double longitude;

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

    private List<ServiceOfferingDto> offerings;

    private Integer yearsExperience;
    private String[] certifications;
    private String[] languages;

    // Additional fields for stylist profile
    private java.util.Set<tech.ceesar.glamme.matching.entity.Stylist.Specialty> specialties;
    private java.util.Set<tech.ceesar.glamme.matching.entity.Stylist.Service> services;
    private java.math.BigDecimal priceRangeMin;
    private java.math.BigDecimal priceRangeMax;
}
