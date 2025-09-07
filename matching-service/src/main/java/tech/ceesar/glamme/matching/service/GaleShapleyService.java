package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.matching.dto.MatchDto;
import tech.ceesar.glamme.matching.dto.MatchingRequest;
import tech.ceesar.glamme.matching.dto.StylistDto;
import tech.ceesar.glamme.matching.entity.Match;
import tech.ceesar.glamme.matching.entity.Stylist;
import tech.ceesar.glamme.matching.repository.MatchRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GaleShapleyService {

    private final StylistRepository stylistRepository;
    private final MatchRepository matchRepository;
    private final ScoringService scoringService;

    @Value("${matching.algorithm.gale-shapley.max-iterations:100}")
    private int maxIterations;

    @Value("${matching.algorithm.gale-shapley.convergence-threshold:0.01}")
    private double convergenceThreshold;

    public List<MatchDto> performStableMatching(MatchingRequest request) {
        try {
            log.info("Performing stable matching for customer: {}", request.getCustomerId());
            
            // Get available stylists
            List<Stylist> availableStylists = getAvailableStylists(request);
            
            if (availableStylists.isEmpty()) {
                log.warn("No available stylists found for customer: {}", request.getCustomerId());
                return Collections.emptyList();
            }
            
            // Create preference lists
            Map<String, List<String>> customerPreferences = createCustomerPreferences(request, availableStylists);
            Map<String, List<String>> stylistPreferences = createStylistPreferences(availableStylists, request);
            
            // Run Gale-Shapley algorithm
            Map<String, String> matches = runGaleShapleyAlgorithm(customerPreferences, stylistPreferences);
            
            // Convert to MatchDto objects
            List<MatchDto> matchDtos = convertToMatchDtos(matches, request.getCustomerId());
            
            // Save matches to database
            saveMatches(matchDtos);
            
            log.info("Generated {} stable matches for customer: {}", matchDtos.size(), request.getCustomerId());
            return matchDtos;
            
        } catch (Exception e) {
            log.error("Failed to perform stable matching for customer: {}", request.getCustomerId(), e);
            return Collections.emptyList();
        }
    }

    private List<Stylist> getAvailableStylists(MatchingRequest request) {
        List<Stylist> stylists = stylistRepository.findByIsAvailableTrueAndIsVerifiedTrue();
        
        // Apply filters
        if (request.getBudgetMin() != null && request.getBudgetMax() != null) {
            stylists = stylists.stream()
                    .filter(s -> s.getHourlyRate() != null && 
                               s.getHourlyRate().compareTo(request.getBudgetMin()) >= 0 &&
                               s.getHourlyRate().compareTo(request.getBudgetMax()) <= 0)
                    .collect(Collectors.toList());
        }
        
        if (request.getMaxDistance() != null && request.getCustomerLatitude() != null && request.getCustomerLongitude() != null) {
            stylists = stylists.stream()
                    .filter(s -> s.getLatitude() != null && s.getLongitude() != null &&
                               calculateDistance(request.getCustomerLatitude(), request.getCustomerLongitude(),
                                               s.getLatitude(), s.getLongitude()) <= request.getMaxDistance())
                    .collect(Collectors.toList());
        }
        
        return stylists;
    }

    private Map<String, List<String>> createCustomerPreferences(MatchingRequest request, List<Stylist> stylists) {
        Map<String, List<String>> preferences = new HashMap<>();
        
        // Score and rank stylists for the customer
        List<StylistDto> scoredStylists = stylists.stream()
                .map(stylist -> {
                    StylistDto dto = convertToStylistDto(stylist);
                    double score = scoringService.calculateCustomerScore(dto, request);
                    dto.setMatchScore(score);
                    return dto;
                })
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
        
        List<String> stylistIds = scoredStylists.stream()
                .map(StylistDto::getId)
                .collect(Collectors.toList());
        
        preferences.put(request.getCustomerId(), stylistIds);
        
        return preferences;
    }

    private Map<String, List<String>> createStylistPreferences(List<Stylist> stylists, MatchingRequest request) {
        Map<String, List<String>> preferences = new HashMap<>();
        
        for (Stylist stylist : stylists) {
            // For simplicity, rank customers by their potential value to the stylist
            // In a real implementation, this would consider stylist preferences
            List<String> customerRanking = Arrays.asList(request.getCustomerId());
            preferences.put(stylist.getId(), customerRanking);
        }
        
        return preferences;
    }

    private Map<String, String> runGaleShapleyAlgorithm(Map<String, List<String>> customerPreferences, 
                                                       Map<String, List<String>> stylistPreferences) {
        Map<String, String> matches = new HashMap<>();
        Map<String, String> stylistMatches = new HashMap<>();
        Map<String, Integer> customerProposalIndex = new HashMap<>();
        
        // Initialize proposal indices
        for (String customer : customerPreferences.keySet()) {
            customerProposalIndex.put(customer, 0);
        }
        
        int iterations = 0;
        boolean stable = false;
        
        while (!stable && iterations < maxIterations) {
            stable = true;
            iterations++;
            
            for (String customer : customerPreferences.keySet()) {
                if (matches.containsKey(customer)) {
                    continue; // Customer already matched
                }
                
                List<String> preferences = customerPreferences.get(customer);
                int proposalIndex = customerProposalIndex.get(customer);
                
                if (proposalIndex < preferences.size()) {
                    String proposedStylist = preferences.get(proposalIndex);
                    
                    if (!stylistMatches.containsKey(proposedStylist)) {
                        // Stylist is free, accept proposal
                        matches.put(customer, proposedStylist);
                        stylistMatches.put(proposedStylist, customer);
                    } else {
                        // Stylist is matched, check if this customer is preferred
                        String currentCustomer = stylistMatches.get(proposedStylist);
                        List<String> stylistPrefs = stylistPreferences.get(proposedStylist);
                        
                        if (isPreferred(stylistPrefs, customer, currentCustomer)) {
                            // Replace current match
                            matches.remove(currentCustomer);
                            matches.put(customer, proposedStylist);
                            stylistMatches.put(proposedStylist, customer);
                            customerProposalIndex.put(currentCustomer, customerProposalIndex.get(currentCustomer) + 1);
                        } else {
                            // Reject proposal
                            customerProposalIndex.put(customer, proposalIndex + 1);
                        }
                    }
                    
                    stable = false;
                }
            }
        }
        
        log.info("Gale-Shapley algorithm completed in {} iterations", iterations);
        return matches;
    }

    private boolean isPreferred(List<String> preferences, String customer1, String customer2) {
        int index1 = preferences.indexOf(customer1);
        int index2 = preferences.indexOf(customer2);
        
        if (index1 == -1) return false;
        if (index2 == -1) return true;
        
        return index1 < index2;
    }

    private List<MatchDto> convertToMatchDtos(Map<String, String> matches, String customerId) {
        List<MatchDto> matchDtos = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : matches.entrySet()) {
            String customer = entry.getKey();
            String stylist = entry.getValue();
            
            if (customer.equals(customerId)) {
                MatchDto matchDto = MatchDto.builder()
                        .customerId(customer)
                        .stylistId(stylist)
                        .status(Match.Status.PENDING)
                        .algorithm(Match.Algorithm.GALE_SHAPLEY)
                        .matchScore(100.0) // Perfect match in stable matching
                        .matchReason("Stable matching algorithm")
                        .build();
                
                matchDtos.add(matchDto);
            }
        }
        
        return matchDtos;
    }

    private void saveMatches(List<MatchDto> matchDtos) {
        for (MatchDto matchDto : matchDtos) {
            Match match = Match.builder()
                    .customerId(matchDto.getCustomerId())
                    .stylistId(matchDto.getStylistId())
                    .matchScore(matchDto.getMatchScore())
                    .matchReason(matchDto.getMatchReason())
                    .status(matchDto.getStatus())
                    .algorithm(matchDto.getAlgorithm())
                    .build();
            
            matchRepository.save(match);
        }
    }

    private StylistDto convertToStylistDto(Stylist stylist) {
        return StylistDto.builder()
                .id(stylist.getId())
                .businessName(stylist.getBusinessName())
                .description(stylist.getDescription())
                .phoneNumber(stylist.getPhoneNumber())
                .email(stylist.getEmail())
                .address(stylist.getAddress())
                .latitude(stylist.getLatitude())
                .longitude(stylist.getLongitude())
                .serviceRadius(stylist.getServiceRadius())
                .hourlyRate(stylist.getHourlyRate())
                .experienceYears(stylist.getExperienceYears())
                .certification(stylist.getCertification())
                .portfolioUrl(stylist.getPortfolioUrl())
                .profileImageUrl(stylist.getProfileImageUrl())
                .isVerified(stylist.getIsVerified())
                .isAvailable(stylist.getIsAvailable())
                .rating(stylist.getRating())
                .reviewCount(stylist.getReviewCount())
                .eloRating(stylist.getEloRating())
                .specialties(stylist.getSpecialties())
                .services(stylist.getServices())
                .createdAt(stylist.getCreatedAt())
                .updatedAt(stylist.getUpdatedAt())
                .build();
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
