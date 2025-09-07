package tech.ceesar.glamme.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylistDto {
    
    private String id;
    
    @NotBlank(message = "Business name is required")
    private String businessName;
    
    private String description;
    
    private String phoneNumber;
    
    private String email;
    
    private String address;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private Integer serviceRadius;
    
    private BigDecimal hourlyRate;
    
    private Integer experienceYears;
    
    private String certification;
    
    private String portfolioUrl;
    
    private String profileImageUrl;
    
    private Boolean isVerified;
    
    private Boolean isAvailable;
    
    private BigDecimal rating;
    
    private Integer reviewCount;
    
    private Integer eloRating;
    
    private Set<Stylist.Specialty> specialties;
    
    private Set<Stylist.Service> services;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Double distanceFromCustomer; // in km
    private Double matchScore;
    private String matchReason;
}