package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.common.service.EventService;
import tech.ceesar.glamme.matching.dto.*;
import tech.ceesar.glamme.matching.entity.CustomerPreference;
import tech.ceesar.glamme.matching.entity.Match;
import tech.ceesar.glamme.matching.entity.Stylist;
import tech.ceesar.glamme.matching.repository.CustomerPreferenceRepository;
import tech.ceesar.glamme.matching.repository.MatchRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class MatchingService {

    private final StylistRepository stylistRepository;
    private final CustomerPreferenceRepository customerPreferenceRepository;
    private final MatchRepository matchRepository;
    private final MatchingAlgorithmService matchingAlgorithmService;
    private final EventService eventService;

    public MatchingService(StylistRepository stylistRepository,
                          CustomerPreferenceRepository customerPreferenceRepository,
                          MatchRepository matchRepository,
                          MatchingAlgorithmService matchingAlgorithmService,
                          EventService eventService) {
        this.stylistRepository = stylistRepository;
        this.customerPreferenceRepository = customerPreferenceRepository;
        this.matchRepository = matchRepository;
        this.matchingAlgorithmService = matchingAlgorithmService;
        this.eventService = eventService;
    }

    public StylistResponse onboardStylist(String userId, StylistOnboardingRequest request) {
        log.info("Onboarding stylist: {}", userId);

        Stylist stylist = Stylist.builder()
                .id(userId)
                .businessName(request.getBusinessName())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .website(request.getWebsite())
                .instagramHandle(request.getInstagramHandle())
                .profileImageUrl(request.getProfileImageUrl())
                .portfolioImages(request.getPortfolioImages())
                .specialties(request.getSpecialties())
                .services(request.getServices())
                .priceRangeMin(request.getPriceRangeMin())
                .priceRangeMax(request.getPriceRangeMax())
                .yearsExperience(request.getYearsExperience())
                .certifications(request.getCertifications())
                .languages(request.getLanguages())
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .eloRating(1500) // Initial Elo rating
                .isVerified(false)
                .isActive(true)
                .lastActive(LocalDateTime.now())
                .build();

        stylist = stylistRepository.save(stylist);

        // Publish stylist onboarding event
        eventService.publishEvent("glamme-bus", "matching-service", "stylist.onboarded", Map.of(
                "stylistId", userId,
                "businessName", request.getBusinessName(),
                "specialties", request.getSpecialties(),
                "services", request.getServices()
        ));

        log.info("Successfully onboarded stylist: {}", userId);
        return mapToStylistResponse(stylist);
    }

    public StylistResponse updateStylistProfile(String userId, StylistOnboardingRequest request) {
        log.info("Updating stylist profile: {}", userId);

        Stylist stylist = stylistRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Stylist not found"));

        stylist.setBusinessName(request.getBusinessName());
        stylist.setDescription(request.getDescription());
        stylist.setLatitude(request.getLatitude());
        stylist.setLongitude(request.getLongitude());
        stylist.setAddress(request.getAddress());
        stylist.setCity(request.getCity());
        stylist.setState(request.getState());
        stylist.setZipCode(request.getZipCode());
        stylist.setPhoneNumber(request.getPhoneNumber());
        stylist.setEmail(request.getEmail());
        stylist.setWebsite(request.getWebsite());
        stylist.setInstagramHandle(request.getInstagramHandle());
        stylist.setProfileImageUrl(request.getProfileImageUrl());
        stylist.setPortfolioImages(request.getPortfolioImages());
        stylist.setSpecialties(request.getSpecialties());
        stylist.setServices(request.getServices());
        stylist.setPriceRangeMin(request.getPriceRangeMin());
        stylist.setPriceRangeMax(request.getPriceRangeMax());
        stylist.setYearsExperience(request.getYearsExperience());
        stylist.setCertifications(request.getCertifications());
        stylist.setLanguages(request.getLanguages());
        stylist.setLastActive(LocalDateTime.now());

        stylist = stylistRepository.save(stylist);

        log.info("Successfully updated stylist profile: {}", userId);
        return mapToStylistResponse(stylist);
    }

    public CustomerPreference updateCustomerPreferences(String customerId, CustomerPreferenceRequest request) {
        log.info("Updating customer preferences: {}", customerId);

        CustomerPreference existingPreference = customerPreferenceRepository.findByCustomerId(customerId).orElse(null);

        CustomerPreference preference;
        if (existingPreference != null) {
            preference = existingPreference;
            preference.setLatitude(request.getLatitude());
            preference.setLongitude(request.getLongitude());
            preference.setMaxDistanceKm(request.getMaxDistanceKm());
            preference.setPreferredSpecialties(request.getPreferredSpecialties());
            preference.setPreferredServices(request.getPreferredServices());
            preference.setPriceRangeMin(request.getPriceRangeMin());
            preference.setPriceRangeMax(request.getPriceRangeMax());
            preference.setMinRating(request.getMinRating());
            preference.setPreferVerified(request.getPreferVerified());
            preference.setPreferExperienced(request.getPreferExperienced());
            preference.setMinYearsExperience(request.getMinYearsExperience());
            preference.setPreferredLanguages(request.getPreferredLanguages());
            preference.setAvailabilityPreferences(request.getAvailabilityPreferences());
        } else {
            preference = CustomerPreference.builder()
                    .customerId(customerId)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .maxDistanceKm(request.getMaxDistanceKm())
                    .preferredSpecialties(request.getPreferredSpecialties())
                    .preferredServices(request.getPreferredServices())
                    .priceRangeMin(request.getPriceRangeMin())
                    .priceRangeMax(request.getPriceRangeMax())
                    .minRating(request.getMinRating())
                    .preferVerified(request.getPreferVerified())
                    .preferExperienced(request.getPreferExperienced())
                    .minYearsExperience(request.getMinYearsExperience())
                    .preferredLanguages(request.getPreferredLanguages())
                    .availabilityPreferences(request.getAvailabilityPreferences())
                    .build();
        }
            
            preference = customerPreferenceRepository.save(preference);

        log.info("Successfully updated customer preferences: {}", customerId);
        return preference;
    }

    public PagedResponse<StylistResponse> recommendStylists(String customerId, int page, int size) {
        log.info("Recommending stylists for customer: {}", customerId);

        CustomerPreference preferences = customerPreferenceRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer preferences not found"));

        List<Stylist> matchingStylists = matchingAlgorithmService.findMatchingStylists(preferences, size * 2);

        // Calculate pagination
        int start = page * size;
        int end = Math.min(start + size, matchingStylists.size());
        List<Stylist> pagedStylists = matchingStylists.subList(start, end);

        List<StylistResponse> stylistResponses = pagedStylists.stream()
                .map(this::mapToStylistResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(
                stylistResponses,
                page,
                size,
                matchingStylists.size()
        );
    }

    public MatchResponse createMatch(String customerId, MatchRequest request) {
        log.info("Creating match for customer: {} with stylist: {}", customerId, request.getStylistId());

        Stylist stylist = stylistRepository.findById(request.getStylistId())
                .orElseThrow(() -> new RuntimeException("Stylist not found"));

        // Check if match already exists
        Optional<Match> existingMatch = matchRepository.findByCustomerIdAndStylistIdAndStatus(
                customerId, request.getStylistId(), Match.Status.PENDING);

        if (existingMatch.isPresent()) {
            throw new RuntimeException("Match already exists");
        }

        // Calculate match score
        CustomerPreference preferences = customerPreferenceRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer preferences not found"));

        List<Stylist> stylists = List.of(stylist);
        List<Stylist> matchingStylists = matchingAlgorithmService.findMatchingStylists(preferences, 1);
        double matchScore = matchingStylists.isEmpty() ? 0.5 : 0.8; // Simplified scoring

        Match match = Match.builder()
                .customerId(customerId)
                .stylistId(request.getStylistId())
                .matchScore(matchScore)
                .matchType(Match.MatchType.MANUAL)
                .status(Match.Status.PENDING)
                .requestedService(request.getRequestedService())
                .preferredDate(request.getPreferredDate())
                .notes(request.getNotes())
                .expiresAt(LocalDateTime.now().plusHours(24)) // 24 hour expiration
                .build();

        match = matchRepository.save(match);

        // Publish match created event
        eventService.publishEvent("glamme-bus", "matching-service", "match.created", Map.of(
                "matchId", match.getId(),
                "customerId", customerId,
                "stylistId", request.getStylistId(),
                "matchScore", matchScore
        ));

        log.info("Successfully created match: {}", match.getId());
        return mapToMatchResponse(match, stylist);
    }

    public MatchResponse respondToMatch(Long matchId, String stylistId, boolean accepted) {
        log.info("Stylist {} responding to match {}: {}", stylistId, matchId, accepted ? "accepted" : "declined");

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getStylistId().equals(stylistId)) {
                throw new RuntimeException("Unauthorized to respond to this match");
            }
            
        if (match.getStatus() != Match.Status.PENDING) {
            throw new RuntimeException("Match is no longer pending");
        }

        if (match.getExpiresAt() != null && LocalDateTime.now().isAfter(match.getExpiresAt())) {
            match.setStatus(Match.Status.EXPIRED);
            matchRepository.save(match);
            throw new RuntimeException("Match has expired");
        }

        match.setStatus(accepted ? Match.Status.ACCEPTED : Match.Status.DECLINED);
        match.setRespondedAt(LocalDateTime.now());
        match = matchRepository.save(match);

        // Update Elo rating based on response
        matchingAlgorithmService.updateEloRating(stylistId, accepted, match.getCustomerId());

        // Publish match response event
        eventService.publishEvent("glamme-bus", "matching-service", 
                accepted ? "match.accepted" : "match.declined", Map.of(
                "matchId", matchId,
                "customerId", match.getCustomerId(),
                "stylistId", stylistId,
                "accepted", accepted
        ));

        Stylist stylist = stylistRepository.findById(stylistId).orElse(null);
        log.info("Successfully processed match response: {}", matchId);
        return mapToMatchResponse(match, stylist);
    }

    public List<MatchResponse> getCustomerMatches(String customerId) {
        List<Match> matches = matchRepository.findByCustomerIdOrderByMatchScoreDesc(customerId);
        return matches.stream()
                .map(match -> {
                    Stylist stylist = stylistRepository.findById(match.getStylistId()).orElse(null);
                    return mapToMatchResponse(match, stylist);
                })
                .collect(Collectors.toList());
    }

    public List<MatchResponse> getStylistMatches(String stylistId) {
        List<Match> matches = matchRepository.findByStylistIdOrderByCreatedAtDesc(stylistId);
        return matches.stream()
                .map(match -> {
                    Stylist stylist = stylistRepository.findById(stylistId).orElse(null);
                    return mapToMatchResponse(match, stylist);
                })
                .collect(Collectors.toList());
    }

    public void cancelMatch(Long matchId, String userId) {
        log.info("Cancelling match: {} by user: {}", matchId, userId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getCustomerId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this match");
        }

        if (match.getStatus() != Match.Status.PENDING) {
            throw new RuntimeException("Match cannot be cancelled");
        }

        match.setStatus(Match.Status.CANCELLED);
        matchRepository.save(match);

        // Publish match cancelled event
        eventService.publishEvent("glamme-bus", "matching-service", "match.cancelled", Map.of(
                "matchId", matchId,
                "customerId", userId,
                "stylistId", match.getStylistId()
        ));

        log.info("Successfully cancelled match: {}", matchId);
    }

    private StylistResponse mapToStylistResponse(Stylist stylist) {
        return StylistResponse.builder()
                .id(stylist.getId())
                .businessName(stylist.getBusinessName())
                .description(stylist.getDescription())
                .latitude(stylist.getLatitude())
                .longitude(stylist.getLongitude())
                .address(stylist.getAddress())
                .city(stylist.getCity())
                .state(stylist.getState())
                .zipCode(stylist.getZipCode())
                .phoneNumber(stylist.getPhoneNumber())
                .email(stylist.getEmail())
                .website(stylist.getWebsite())
                .instagramHandle(stylist.getInstagramHandle())
                .profileImageUrl(stylist.getProfileImageUrl())
                .portfolioImages(stylist.getPortfolioImages())
                .specialties(stylist.getSpecialties())
                .services(stylist.getServices())
                .priceRangeMin(stylist.getPriceRangeMin())
                .priceRangeMax(stylist.getPriceRangeMax())
                .averageRating(stylist.getAverageRating())
                .totalReviews(stylist.getTotalReviews())
                .eloRating(stylist.getEloRating())
                .isVerified(stylist.getIsVerified())
                .isActive(stylist.getIsActive())
                .yearsExperience(stylist.getYearsExperience())
                .certifications(stylist.getCertifications())
                .languages(stylist.getLanguages())
                .createdAt(stylist.getCreatedAt())
                .lastActive(stylist.getLastActive())
                .build();
    }

    private MatchResponse mapToMatchResponse(Match match, Stylist stylist) {
        MatchResponse.MatchResponseBuilder builder = MatchResponse.builder()
                .id(match.getId())
                .customerId(match.getCustomerId())
                .stylistId(match.getStylistId())
                .matchScore(match.getMatchScore())
                .matchType(match.getMatchType())
                .status(match.getStatus())
                .requestedService(match.getRequestedService())
                .preferredDate(match.getPreferredDate())
                .notes(match.getNotes())
                .createdAt(match.getCreatedAt())
                .expiresAt(match.getExpiresAt())
                .respondedAt(match.getRespondedAt());

        if (stylist != null) {
            builder.stylist(mapToStylistResponse(stylist));
        }

        return builder.build();
    }
}