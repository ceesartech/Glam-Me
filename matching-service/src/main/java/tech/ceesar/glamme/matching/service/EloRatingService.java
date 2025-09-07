package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.service.CacheService;
import tech.ceesar.glamme.matching.entity.Stylist;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EloRatingService {

    private final StylistRepository stylistRepository;
    private final CacheService cacheService;

    @Value("${matching.algorithm.elo.initial-rating:1200}")
    private int initialRating;

    @Value("${matching.algorithm.elo.k-factor:32}")
    private int kFactor;

    @Value("${matching.algorithm.elo.min-rating:100}")
    private int minRating;

    @Value("${matching.algorithm.elo.max-rating:3000}")
    private int maxRating;

    public void updateEloRating(String stylistId, boolean won, String opponentId) {
        try {
            Stylist stylist = stylistRepository.findById(stylistId)
                    .orElseThrow(() -> new RuntimeException("Stylist not found: " + stylistId));

            int currentRating = stylist.getEloRating() != null ? stylist.getEloRating() : initialRating;
            int opponentRating = getOpponentRating(opponentId);
            
            double expectedScore = calculateExpectedScore(currentRating, opponentRating);
            double actualScore = won ? 1.0 : 0.0;
            
            int newRating = calculateNewRating(currentRating, expectedScore, actualScore);
            newRating = Math.max(minRating, Math.min(maxRating, newRating));
            
            stylist.setEloRating(newRating);
            stylistRepository.save(stylist);
            
            // Update cache
            String cacheKey = "stylist:elo:" + stylistId;
            cacheService.set(cacheKey, newRating, Duration.ofHours(1));
            
            log.info("Updated Elo rating for stylist {}: {} -> {} (won: {})", 
                    stylistId, currentRating, newRating, won);
            
        } catch (Exception e) {
            log.error("Failed to update Elo rating for stylist: {}", stylistId, e);
        }
    }

    public int getEloRating(String stylistId) {
        try {
            String cacheKey = "stylist:elo:" + stylistId;
            Optional<Integer> cachedRating = cacheService.get(cacheKey, Integer.class);

            if (cachedRating.isPresent()) {
                return cachedRating.get();
            }
            
            Stylist stylist = stylistRepository.findById(stylistId)
                    .orElseThrow(() -> new RuntimeException("Stylist not found: " + stylistId));
            
            int rating = stylist.getEloRating() != null ? stylist.getEloRating() : initialRating;
            cacheService.set(cacheKey, rating, Duration.ofHours(1));
            
            return rating;
            
        } catch (Exception e) {
            log.error("Failed to get Elo rating for stylist: {}", stylistId, e);
            return initialRating;
        }
    }

    public double calculateMatchScore(int stylistRating, int customerRating) {
        double expectedScore = calculateExpectedScore(stylistRating, customerRating);
        return expectedScore * 100; // Convert to percentage
    }

    private double calculateExpectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
    }

    private int calculateNewRating(int currentRating, double expectedScore, double actualScore) {
        return (int) Math.round(currentRating + kFactor * (actualScore - expectedScore));
    }

    private int getOpponentRating(String opponentId) {
        // In a real implementation, this would get the customer's rating
        // For now, we'll use a default rating
        return initialRating;
    }

    /**
     * Get Elo adjustment for a match between customer and stylist
     */
    public double getEloAdjustment(String customerId, String stylistId) {
        // This is a simplified implementation
        // In a real system, this would calculate based on customer rating vs stylist rating
        return 0.0; // No adjustment for now
    }

    public void initializeEloRating(String stylistId) {
        try {
            Stylist stylist = stylistRepository.findById(stylistId)
                    .orElseThrow(() -> new RuntimeException("Stylist not found: " + stylistId));
            
            if (stylist.getEloRating() == null) {
                stylist.setEloRating(initialRating);
                stylistRepository.save(stylist);
                
                String cacheKey = "stylist:elo:" + stylistId;
                cacheService.set(cacheKey, initialRating, Duration.ofHours(1));
                
                log.info("Initialized Elo rating for stylist {}: {}", stylistId, initialRating);
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize Elo rating for stylist: {}", stylistId, e);
        }
    }
}
