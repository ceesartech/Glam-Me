package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.matching.dto.MatchingRequest;
import tech.ceesar.glamme.matching.dto.StylistDto;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final EloRatingService eloRatingService;

    @Value("${matching.scoring.weights.elo:0.4}")
    private double eloWeight;

    @Value("${matching.scoring.weights.distance:0.3}")
    private double distanceWeight;

    @Value("${matching.scoring.weights.price:0.2}")
    private double priceWeight;

    @Value("${matching.scoring.weights.availability:0.1}")
    private double availabilityWeight;

    public double calculateCustomerScore(StylistDto stylist, MatchingRequest request) {
        try {
            double eloScore = calculateEloScore(stylist);
            double distanceScore = calculateDistanceScore(stylist, request);
            double priceScore = calculatePriceScore(stylist, request);
            double availabilityScore = calculateAvailabilityScore(stylist);
            
            double totalScore = (eloScore * eloWeight) + 
                               (distanceScore * distanceWeight) + 
                               (priceScore * priceWeight) + 
                               (availabilityScore * availabilityWeight);
            
            log.debug("Calculated score for stylist {}: {} (elo: {}, distance: {}, price: {}, availability: {})", 
                    stylist.getId(), totalScore, eloScore, distanceScore, priceScore, availabilityScore);
            
            return totalScore;
            
        } catch (Exception e) {
            log.error("Failed to calculate score for stylist: {}", stylist.getId(), e);
            return 0.0;
        }
    }

    public double calculateStylistScore(Stylist stylist, String customerId) {
        try {
            // For stylists, we might consider different factors
            // For now, we'll use a combination of rating and experience
            double ratingScore = calculateRatingScore(stylist);
            double experienceScore = calculateExperienceScore(stylist);
            
            return (ratingScore * 0.7) + (experienceScore * 0.3);
            
        } catch (Exception e) {
            log.error("Failed to calculate stylist score for customer: {}", customerId, e);
            return 0.0;
        }
    }

    private double calculateEloScore(StylistDto stylist) {
        int eloRating = stylist.getEloRating() != null ? stylist.getEloRating() : 1200;
        
        // Normalize Elo rating to 0-100 scale
        // Assuming Elo ratings range from 100 to 3000
        return Math.max(0, Math.min(100, (eloRating - 100) / 29.0));
    }

    private double calculateDistanceScore(StylistDto stylist, MatchingRequest request) {
        if (request.getCustomerLatitude() == null || request.getCustomerLongitude() == null ||
            stylist.getLatitude() == null || stylist.getLongitude() == null) {
            return 50.0; // Default score if location data is missing
        }
        
        double distance = calculateDistance(
                request.getCustomerLatitude(), request.getCustomerLongitude(),
                stylist.getLatitude(), stylist.getLongitude()
        );
        
        Integer maxDistance = request.getMaxDistance() != null ? request.getMaxDistance() : 50;
        
        if (distance > maxDistance) {
            return 0.0; // Outside preferred range
        }
        
        // Score decreases as distance increases
        return Math.max(0, 100 - (distance / maxDistance) * 100);
    }

    private double calculatePriceScore(StylistDto stylist, MatchingRequest request) {
        if (stylist.getHourlyRate() == null) {
            return 50.0; // Default score if price is not set
        }
        
        BigDecimal budgetMin = request.getBudgetMin();
        BigDecimal budgetMax = request.getBudgetMax();
        
        if (budgetMin == null || budgetMax == null) {
            return 50.0; // Default score if budget is not specified
        }
        
        BigDecimal hourlyRate = stylist.getHourlyRate();
        
        if (hourlyRate.compareTo(budgetMin) < 0) {
            return 100.0; // Below budget - excellent
        } else if (hourlyRate.compareTo(budgetMax) <= 0) {
            // Within budget - score based on how close to minimum
            double ratio = hourlyRate.subtract(budgetMin).doubleValue() / 
                          budgetMax.subtract(budgetMin).doubleValue();
            return 100 - (ratio * 50); // 100 to 50 range
        } else {
            return 0.0; // Above budget
        }
    }

    private double calculateAvailabilityScore(StylistDto stylist) {
        if (stylist.getIsAvailable() == null) {
            return 50.0; // Default score if availability is unknown
        }
        
        return stylist.getIsAvailable() ? 100.0 : 0.0;
    }

    private double calculateRatingScore(Stylist stylist) {
        if (stylist.getRating() == null) {
            return 50.0; // Default score if rating is not available
        }
        
        // Convert 5-star rating to 0-100 scale
        return stylist.getRating().doubleValue() * 20;
    }

    private double calculateExperienceScore(Stylist stylist) {
        if (stylist.getExperienceYears() == null) {
            return 50.0; // Default score if experience is unknown
        }
        
        int years = stylist.getExperienceYears();
        
        // Score increases with experience, capped at 100
        return Math.min(100, years * 10);
    }

    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        // Haversine formula for calculating distance between two points
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lngDistance = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
