package tech.ceesar.glamme.image.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HairstyleSearchRequest {
    
    private String query;          // Text search query
    private String category;       // Category filter (e.g., "braids", "cuts", "color")
    private String difficulty;     // Difficulty level filter
    private Integer minTime;       // Minimum estimated time in minutes
    private Integer maxTime;       // Maximum estimated time in minutes
    private Integer limit;         // Number of results to return
    private String sortBy;         // Sort field (popularity, trending, alphabetical)
    private String sortOrder;      // Sort order (asc, desc)
}
