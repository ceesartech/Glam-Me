package tech.ceesar.glamme.image.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hairstyle_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HairstyleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false)
    private String category; // braids, cuts, color, updos, etc.

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "preview_url")
    private String previewUrl;

    @ElementCollection
    @CollectionTable(name = "hairstyle_tags", joinColumns = @JoinColumn(name = "hairstyle_id"))
    @Column(name = "tag")
    private List<String> tags;

    @Column(name = "difficulty")
    private String difficulty; // Easy, Medium, Hard

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTime;

    @Column(name = "popularity_score")
    @Builder.Default
    private Double popularityScore = 0.0;

    @Column(name = "usage_count")
    @Builder.Default
    private Long usageCount = 0L;

    @Column(name = "success_rate")
    @Builder.Default
    private Double successRate = 0.0;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @ElementCollection
    @CollectionTable(name = "hairstyle_keywords", joinColumns = @JoinColumn(name = "hairstyle_id"))
    @Column(name = "keyword")
    private List<String> styleKeywords;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods for scoring
    public void incrementUsage() {
        this.usageCount++;
        recalculatePopularityScore();
    }

    public void updateSuccessRate(boolean success) {
        // Simple running average - in production, you'd use a more sophisticated approach
        double currentTotal = this.successRate * this.usageCount;
        this.usageCount++;
        this.successRate = (currentTotal + (success ? 1.0 : 0.0)) / this.usageCount;
        recalculatePopularityScore();
    }

    private void recalculatePopularityScore() {
        // Weighted combination of usage count, success rate, and recency
        double usageWeight = Math.min(100.0, Math.log(usageCount + 1) * 10);
        double successWeight = successRate * 100;
        double recencyWeight = calculateRecencyScore();
        
        this.popularityScore = (usageWeight * 0.4) + (successWeight * 0.4) + (recencyWeight * 0.2);
    }

    private double calculateRecencyScore() {
        LocalDateTime now = LocalDateTime.now();
        long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(createdAt, now);
        
        if (daysAgo < 7) return 100.0;
        if (daysAgo < 30) return 80.0;
        if (daysAgo < 90) return 60.0;
        if (daysAgo < 365) return 40.0;
        return 20.0;
    }
}
