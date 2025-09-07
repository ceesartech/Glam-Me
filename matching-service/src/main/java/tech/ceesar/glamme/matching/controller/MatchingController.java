package tech.ceesar.glamme.matching.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.common.dto.ApiResponse;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.matching.dto.*;
import tech.ceesar.glamme.matching.entity.CustomerPreference;
import tech.ceesar.glamme.matching.entity.Stylist;
import tech.ceesar.glamme.matching.service.MatchingService;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/stylists/onboard")
    public ResponseEntity<ApiResponse<StylistResponse>> onboardStylist(
            @Valid @RequestBody StylistOnboardingRequest request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            StylistResponse response = matchingService.onboardStylist(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Stylist onboarded successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/stylists/profile")
    public ResponseEntity<ApiResponse<StylistResponse>> updateStylistProfile(
            @Valid @RequestBody StylistOnboardingRequest request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            StylistResponse response = matchingService.updateStylistProfile(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Stylist profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/customers/preferences")
    public ResponseEntity<ApiResponse<CustomerPreference>> updateCustomerPreferences(
            @Valid @RequestBody CustomerPreferenceRequest request,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            CustomerPreference response = matchingService.updateCustomerPreferences(customerId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Customer preferences updated successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<PagedResponse<StylistResponse>>> recommendStylists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            PagedResponse<StylistResponse> response = matchingService.recommendStylists(customerId, page, size);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/matches")
    public ResponseEntity<ApiResponse<MatchResponse>> createMatch(
            @Valid @RequestBody MatchRequest request,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            MatchResponse response = matchingService.createMatch(customerId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Match created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/matches/{matchId}/respond")
    public ResponseEntity<ApiResponse<MatchResponse>> respondToMatch(
            @PathVariable Long matchId,
            @RequestParam boolean accepted,
            Authentication authentication
    ) {
        try {
            String stylistId = authentication.getName();
            MatchResponse response = matchingService.respondToMatch(matchId, stylistId, accepted);
            return ResponseEntity.ok(ApiResponse.success(response, 
                    accepted ? "Match accepted successfully" : "Match declined successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/matches/customer")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getCustomerMatches(
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            List<MatchResponse> response = matchingService.getCustomerMatches(customerId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/matches/stylist")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getStylistMatches(
            Authentication authentication
    ) {
        try {
            String stylistId = authentication.getName();
            List<MatchResponse> response = matchingService.getStylistMatches(stylistId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<ApiResponse<Void>> cancelMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            matchingService.cancelMatch(matchId, userId);
            return ResponseEntity.ok(ApiResponse.success(null, "Match cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // === IMAGE SERVICE INTEGRATION ENDPOINTS ===

    /**
     * Create matches based on image processing results
     */
    @PostMapping("/matches/image-based")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> createImageBasedMatches(
            @Valid @RequestBody ImageBasedMatchRequest request,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            request.setCustomerId(customerId);
            
            List<MatchResponse> response = matchingService.createImageBasedMatches(request);
            return ResponseEntity.ok(ApiResponse.success(response, 
                    "Image-based matches created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create direct booking (bypass matching)
     */
    @PostMapping("/matches/direct-booking")
    public ResponseEntity<ApiResponse<MatchResponse>> createDirectBooking(
            @Valid @RequestBody DirectBookingRequest request,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            request.setCustomerId(customerId);
            
            MatchResponse response = matchingService.createDirectBooking(request);
            return ResponseEntity.ok(ApiResponse.success(response, 
                    "Direct booking created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get hairstyle-specific matches
     */
    @GetMapping("/matches/hairstyle")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getHairstyleMatches(
            @RequestParam String hairstyleQuery,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            List<MatchResponse> response = matchingService.getHairstyleMatches(
                    customerId, hairstyleQuery, latitude, longitude, limit);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Quick stylist search by hairstyle (no authentication required)
     */
    @GetMapping("/stylists/by-hairstyle")
    public ResponseEntity<ApiResponse<List<StylistResponse>>> searchStylistsByHairstyle(
            @RequestParam String hairstyle,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "25") Integer maxDistanceKm,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        try {
            // Create temporary preferences for public search
            CustomerPreference tempPreferences = CustomerPreference.builder()
                    .customerId("anonymous")
                    .latitude(BigDecimal.valueOf(latitude))
                    .longitude(BigDecimal.valueOf(longitude))
                    .maxDistanceKm(maxDistanceKm)
                    .build();
                    
            List<Stylist> stylists = matchingService.findStylistsByHairstyleQuery(
                    hairstyle, tempPreferences, limit);
                    
            List<StylistResponse> response = stylists.stream()
                    .map(this::mapToStylistResponse)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // Helper method to map Stylist to StylistResponse
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
}