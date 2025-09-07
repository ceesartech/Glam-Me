package tech.ceesar.glamme.image.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.image.entity.HairstyleTemplate;

import java.util.List;

@Repository
public interface HairstyleTemplateRepository extends JpaRepository<HairstyleTemplate, String> {

    /**
     * Find hairstyles by category, ordered by popularity
     */
    List<HairstyleTemplate> findByCategoryOrderByPopularityScoreDesc(String category, Pageable pageable);

    /**
     * Find all hairstyles ordered by popularity
     */
    List<HairstyleTemplate> findAllByOrderByPopularityScoreDesc(Pageable pageable);

    /**
     * Find hairstyles by multiple categories
     */
    List<HairstyleTemplate> findByCategoryInOrderByPopularityScoreDesc(List<String> categories, Pageable pageable);

    /**
     * Search hairstyles by keywords in name, description, or tags
     */
    @Query("SELECT h FROM HairstyleTemplate h WHERE " +
           "LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT t FROM h.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "EXISTS (SELECT k FROM h.styleKeywords k WHERE LOWER(k) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY h.popularityScore DESC")
    List<HairstyleTemplate> searchByKeywords(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find trending hairstyles (high popularity, recently created or updated)
     */
    @Query("SELECT h FROM HairstyleTemplate h WHERE " +
           "h.createdAt >= CURRENT_DATE - 30 AND h.popularityScore > 60 " +
           "ORDER BY h.popularityScore DESC, h.createdAt DESC")
    List<HairstyleTemplate> findTrendingHairstyles(Pageable pageable);

    /**
     * Find hairstyles by difficulty level
     */
    List<HairstyleTemplate> findByDifficultyOrderByPopularityScoreDesc(String difficulty, Pageable pageable);

    /**
     * Find hairstyles by estimated time range
     */
    @Query("SELECT h FROM HairstyleTemplate h WHERE " +
           "h.estimatedTime BETWEEN :minTime AND :maxTime " +
           "ORDER BY h.popularityScore DESC")
    List<HairstyleTemplate> findByEstimatedTimeBetween(@Param("minTime") Integer minTime, 
                                                       @Param("maxTime") Integer maxTime, 
                                                       Pageable pageable);

    /**
     * Find most popular hairstyles (top 20% by popularity score)
     */
    @Query("SELECT h FROM HairstyleTemplate h WHERE h.popularityScore > 80 " +
           "ORDER BY h.popularityScore DESC")
    List<HairstyleTemplate> findMostPopular(Pageable pageable);

    /**
     * Find hairstyles by style keywords (for matching with stylist specialties)
     */
    @Query("SELECT h FROM HairstyleTemplate h WHERE " +
           "EXISTS (SELECT k FROM h.styleKeywords k WHERE k IN :keywords) " +
           "ORDER BY h.popularityScore DESC")
    List<HairstyleTemplate> findByStyleKeywords(@Param("keywords") List<String> keywords, Pageable pageable);
}
