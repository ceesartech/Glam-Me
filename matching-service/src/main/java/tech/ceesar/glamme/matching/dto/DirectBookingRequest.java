package tech.ceesar.glamme.matching.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectBookingRequest {
    
    private String customerId;
    private String stylistId;       // Specific stylist ID (optional)
    private String hairstyleName;   // Known hairstyle name
    private String serviceType;     // Service category
    private String notes;
    private LocalDateTime preferredDate;
    private String imageJobId;      // Optional - if they also used image service
    private String hairstyleId;     // Optional - specific hairstyle template
    
    // Quick booking flags
    private Boolean skipMatching;   // True if they want to book directly
    private Boolean useImageResult; // True if they want to use image generation result
}
