package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.matching.entity.CustomerPreference;
import tech.ceesar.glamme.matching.entity.Match;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.util.List;
import java.util.Map;

/**
 * Service for matching algorithms between customers and stylists
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingAlgorithmService {

    private final EloRatingService eloRatingService;

    /**
     * Calculate match score between a customer and stylist
     */
    public double calculateMatchScore(String customerId, String stylistId,
                                    CustomerPreference preferences, Stylist stylist) {
        try {
            double score = 0.0;

            // Location matching (30%)
            double locationScore = calculateLocationScore(preferences, stylist);
            score += locationScore * 0.3;

            // Specialty/Service matching (25%)
            double specialtyScore = calculateSpecialtyScore(preferences, stylist);
            score += specialtyScore * 0.25;

            // Price range matching (20%)
            double priceScore = calculatePriceScore(preferences, stylist);
            score += priceScore * 0.2;

            // Rating/Experience matching (15%)
            double ratingScore = calculateRatingScore(preferences, stylist);
            score += ratingScore * 0.15;

            // Elo rating based adjustment (10%)
            double eloAdjustment = eloRatingService.getEloAdjustment(customerId, stylistId);
            score += eloAdjustment * 0.1;

            return Math.max(0.0, Math.min(100.0, score));

        } catch (Exception e) {
            log.error("Error calculating match score for customer {} and stylist {}", customerId, stylistId, e);
            return 0.0;
        }
    }

    private double calculateLocationScore(CustomerPreference preferences, Stylist stylist) {
        if (preferences.getLatitude() == null || preferences.getLongitude() == null ||
            stylist.getLatitude() == null || stylist.getLongitude() == null) {
            return 50.0; // Neutral score if location not available
        }

        // Simple distance calculation (in a real app, use proper geospatial calculation)
        double distance = calculateDistance(
            preferences.getLatitude().doubleValue(),
            preferences.getLongitude().doubleValue(),
            stylist.getLatitude().doubleValue(),
            stylist.getLongitude().doubleValue()
        );

        Integer maxDistance = preferences.getMaxDistanceKm();
        if (maxDistance == null || distance <= maxDistance) {
            return 100.0 - (distance / 50.0) * 20.0; // Decrease score with distance
        } else {
            return Math.max(0.0, 100.0 - (distance - maxDistance) * 5.0);
        }
    }

    private double calculateSpecialtyScore(CustomerPreference preferences, Stylist stylist) {
        if (preferences.getPreferredSpecialties() == null || preferences.getPreferredSpecialties().isEmpty()) {
            return 75.0; // Good default score
        }

        long matchingSpecialties = preferences.getPreferredSpecialties().stream()
                .filter(stylist.getSpecialties()::contains)
                .count();

        return (double) matchingSpecialties / preferences.getPreferredSpecialties().size() * 100.0;
    }

    private double calculatePriceScore(CustomerPreference preferences, Stylist stylist) {
        if (preferences.getPriceRangeMin() == null || preferences.getPriceRangeMax() == null ||
            stylist.getPriceRangeMin() == null || stylist.getPriceRangeMax() == null) {
            return 75.0; // Neutral score if price info not available
        }

        // Check if stylist price range overlaps with customer preference
        boolean overlaps = stylist.getPriceRangeMax().compareTo(preferences.getPriceRangeMin()) >= 0 &&
                          stylist.getPriceRangeMin().compareTo(preferences.getPriceRangeMax()) <= 0;

        return overlaps ? 100.0 : 25.0;
    }

    private double calculateRatingScore(CustomerPreference preferences, Stylist stylist) {
        double score = 50.0; // Base score

        // Rating score
        if (stylist.getAverageRating() != null) {
            if (preferences.getMinRating() != null) {
                score += stylist.getAverageRating().compareTo(preferences.getMinRating()) >= 0 ? 25.0 : -25.0;
            } else {
                score += stylist.getAverageRating().doubleValue() * 2.5; // 0-100 scale
            }
        }

        // Experience score
        if (preferences.getPreferExperienced() != null && preferences.getPreferExperienced()) {
            if (stylist.getYearsExperience() != null && stylist.getYearsExperience() >= 3) {
                score += 15.0;
            }
        }

        // Verification score
        if (preferences.getPreferVerified() != null && preferences.getPreferVerified()) {
            if (stylist.getIsVerified() != null && stylist.getIsVerified()) {
                score += 10.0;
            }
        }

        return Math.max(0.0, Math.min(100.0, score));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula for distance calculation
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // convert to km
    }

    /**
     * Get recommended stylists for a customer
     */
    public List<String> getRecommendedStylistIds(String customerId, int limit) {
        // This would implement collaborative filtering or other recommendation algorithms
        // For now, return empty list - to be implemented based on business requirements
        log.info("Getting recommended stylists for customer: {}", customerId);
        return List.of();
    }

    /**
     * Find matching stylists based on customer preferences
     */
    public List<Stylist> findMatchingStylists(CustomerPreference preferences, int limit) {
        // This is a placeholder implementation
        // In a real implementation, this would use sophisticated matching algorithms
        log.info("Finding matching stylists for customer preferences, limit: {}", limit);
        return List.of(); // Return empty list for now
    }

    /**
     * Update Elo rating based on match outcome
     */
    public void updateEloRating(String stylistId, boolean accepted, String customerId) {
        log.info("Updating Elo rating for stylist {} based on match acceptance: {}", stylistId, accepted);
        // This would integrate with EloRatingService
    }

    /**
     * Update match algorithm based on user feedback
     */
    public void updateAlgorithmWeights(Match.Algorithm algorithm, Map<String, Double> weights) {
        log.info("Updating algorithm weights for {}: {}", algorithm, weights);
        // Implementation would update algorithm parameters based on performance
    }
}
