package tech.ceesar.glamme.image.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HairstyleSearchResponse {
    
    private String id;
    private String name;
    private String description;
    private String category;
    private String imageUrl;         // Main hairstyle image
    private String previewUrl;       // Thumbnail/preview image
    private List<String> tags;       // Search tags
    private String difficulty;       // Easy, Medium, Hard
    private Integer estimatedTime;   // Time in minutes
    private Double popularityScore;  // 0-100 popularity score
    private Boolean isPopular;       // True if in top 20% popular
    private Boolean isTrending;      // True if trending in last 30 days
    
    // Additional metadata for matching
    private String promptTemplate;   // AI prompt template for generation
    private List<String> styleKeywords; // Keywords for matching with stylists
}
