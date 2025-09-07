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
public class ImageBasedMatchRequest {
    
    private String customerId;
    private String imageJobId;      // ID from image service job
    private String hairstyleId;     // Selected hairstyle template ID
    private String notes;           // Additional notes from customer
    private LocalDateTime preferredDate;
    private Double maxDistance;     // Override customer's default max distance
    private Double minPrice;        // Price range preferences
    private Double maxPrice;
    private String requestedService; // Specific service type
}
