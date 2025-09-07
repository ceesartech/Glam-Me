package tech.ceesar.glamme.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
// OpenSearch integration simplified for now - using database search
import tech.ceesar.glamme.image.dto.HairstyleSearchRequest;
import tech.ceesar.glamme.image.dto.HairstyleSearchResponse;
import tech.ceesar.glamme.image.entity.HairstyleTemplate;
import tech.ceesar.glamme.image.repository.HairstyleTemplateRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HairstyleSearchService {

    private final HairstyleTemplateRepository hairstyleRepository;

    @Value("${aws.opensearch.hairstyles-index:hairstyles}")
    private String hairstylesIndex;

    /**
     * Search hairstyles by type, category, or text description
     */
    public List<HairstyleSearchResponse> searchHairstyles(HairstyleSearchRequest request) {
        try {
            log.info("Searching hairstyles with query: {}", request.getQuery());

            // For now, use database search directly
            // TODO: Add OpenSearch integration for better semantic search
            return searchWithDatabase(request);

        } catch (Exception e) {
            log.error("Error searching hairstyles", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Get popular hairstyles by category
     */
    public List<HairstyleSearchResponse> getPopularHairstyles(String category, int limit) {
        List<HairstyleTemplate> templates;
        
        if (category != null && !category.trim().isEmpty()) {
            templates = hairstyleRepository.findByCategoryOrderByPopularityScoreDesc(category, 
                org.springframework.data.domain.PageRequest.of(0, limit));
        } else {
            templates = hairstyleRepository.findAllByOrderByPopularityScoreDesc(
                org.springframework.data.domain.PageRequest.of(0, limit));
        }

        return templates.stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get trending hairstyles
     */
    public List<HairstyleSearchResponse> getTrendingHairstyles(int limit) {
        List<HairstyleTemplate> templates = hairstyleRepository
                .findTrendingHairstyles(org.springframework.data.domain.PageRequest.of(0, limit));

        return templates.stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get hairstyles by specific categories
     */
    public List<HairstyleSearchResponse> getHairstylesByCategories(List<String> categories, int limit) {
        List<HairstyleTemplate> templates = hairstyleRepository
                .findByCategoryInOrderByPopularityScoreDesc(categories, 
                    org.springframework.data.domain.PageRequest.of(0, limit));

        return templates.stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());
    }

    // OpenSearch integration removed for simplicity
    // Can be added back later with proper OpenSearch configuration

    private List<HairstyleSearchResponse> searchWithDatabase(HairstyleSearchRequest request) {
        List<HairstyleTemplate> templates;
        
        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            templates = hairstyleRepository.searchByKeywords(request.getQuery().trim(), 
                org.springframework.data.domain.PageRequest.of(0, request.getLimit() != null ? request.getLimit() : 20));
        } else if (request.getCategory() != null) {
            templates = hairstyleRepository.findByCategoryOrderByPopularityScoreDesc(request.getCategory(),
                org.springframework.data.domain.PageRequest.of(0, request.getLimit() != null ? request.getLimit() : 20));
        } else {
            templates = hairstyleRepository.findAllByOrderByPopularityScoreDesc(
                org.springframework.data.domain.PageRequest.of(0, request.getLimit() != null ? request.getLimit() : 20));
        }

        return templates.stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());
    }

    // OpenSearch query methods removed - using database search only for now

    private HairstyleSearchResponse mapToSearchResponse(HairstyleTemplate template) {
        return HairstyleSearchResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .imageUrl(template.getImageUrl())
                .previewUrl(template.getPreviewUrl())
                .tags(template.getTags())
                .difficulty(template.getDifficulty())
                .estimatedTime(template.getEstimatedTime())
                .popularityScore(template.getPopularityScore())
                .isPopular(template.getPopularityScore() > 80)
                .isTrending(template.getCreatedAt().isAfter(
                    java.time.LocalDateTime.now().minusDays(30)) && template.getPopularityScore() > 60)
                .build();
    }
}
