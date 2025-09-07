package tech.ceesar.glamme.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for stylist information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistResponse {

    private String id;
    private String businessName;
    private String description;

    private BigDecimal latitude;
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
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer eloRating;

    private Boolean isVerified;
    private Boolean isActive;
    private Integer yearsExperience;

    private String[] certifications;
    private String[] languages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActive;
}
