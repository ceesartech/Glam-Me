package tech.ceesar.glamme.matching.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.ceesar.glamme.common.enums.SubscriptionType;
import tech.ceesar.glamme.matching.dto.*;
import tech.ceesar.glamme.matching.entity.*;
import tech.ceesar.glamme.matching.repository.*;
import tech.ceesar.glamme.common.service.EventService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchingServiceTest {
    @Mock StylistRepository stylistRepository;
    @Mock CustomerPreferenceRepository customerPreferenceRepository;
    @Mock MatchRepository matchRepository;
    @Mock MatchingAlgorithmService matchingAlgorithmService;
    @Mock EventService eventService;
    @InjectMocks MatchingService matchingService;

    private Stylist stylist1, stylist2;
    private CustomerPreference customerPreference;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        
        // Setting up test stylists
        stylist1 = Stylist.builder()
                .id("stylist-1")
                .businessName("Hair Studio 1")
                .latitude(BigDecimal.valueOf(40.0))
                .longitude(BigDecimal.valueOf(-105.0))
                .eloRating(1600)
                .priceRangeMin(BigDecimal.valueOf(50))
                .priceRangeMax(BigDecimal.valueOf(100))
                .isActive(true)
                .build();

        stylist2 = Stylist.builder()
                .id("stylist-2")
                .businessName("Hair Studio 2")
                .latitude(BigDecimal.valueOf(40.1))
                .longitude(BigDecimal.valueOf(-105.1))
                .eloRating(1700)
                .priceRangeMin(BigDecimal.valueOf(70))
                .priceRangeMax(BigDecimal.valueOf(120))
                .isActive(true)
                .build();

        // Customer preferences
        customerPreference = CustomerPreference.builder()
                .customerId("customer-1")
                .latitude(BigDecimal.valueOf(40.0))
                .longitude(BigDecimal.valueOf(-105.0))
                .maxDistanceKm(25)
                .priceRangeMin(BigDecimal.valueOf(50))
                .priceRangeMax(BigDecimal.valueOf(200))
                .build();
    }

    @Test
    void createMatch_shouldCreatePendingMatch() {
        // Arrange
        String customerId = "customer-1";
        MatchRequest request = MatchRequest.builder()
                .customerId(customerId)
                .stylistId("stylist-1")
                .notes("Test booking")
                .build();

        when(stylistRepository.findById("stylist-1")).thenReturn(Optional.of(stylist1));
        when(customerPreferenceRepository.findByCustomerId(customerId)).thenReturn(Optional.of(customerPreference));
        when(matchRepository.findByCustomerIdAndStylistIdAndStatus(customerId, "stylist-1", Match.Status.PENDING))
                .thenReturn(Optional.empty());
        when(matchingAlgorithmService.findMatchingStylists(any(), eq(1))).thenReturn(List.of(stylist1));

        Match savedMatch = Match.builder()
                .id(1L)
                .customerId(customerId)
                .stylistId("stylist-1")
                .matchScore(0.8)
                .status(Match.Status.PENDING)
                .matchType(Match.MatchType.MANUAL)
                .build();
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        doNothing().when(eventService).publishEvent(anyString(), anyString(), anyString(), any());

        // Act
        MatchResponse result = matchingService.createMatch(customerId, request);

        // Assert
        assertEquals(savedMatch.getId(), result.getId());
        assertEquals(Match.Status.PENDING, result.getStatus());
        verify(matchRepository).save(any(Match.class));
        verify(eventService).publishEvent(anyString(), anyString(), anyString(), any());
    }

    @Test
    void getHairstyleMatches_shouldReturnMatchingStylists() {
        // Arrange
        String customerId = "customer-1";
        String hairstyleQuery = "braids";
        
        when(customerPreferenceRepository.findByCustomerId(customerId)).thenReturn(Optional.of(customerPreference));
        when(stylistRepository.findBySpecialtiesContainingIgnoreCase(hairstyleQuery)).thenReturn(List.of(stylist1, stylist2));
        when(matchingAlgorithmService.calculateMatchScore(eq(customerId), anyString(), any(), any())).thenReturn(85.0);

        // Act
        List<MatchResponse> result = matchingService.getHairstyleMatches(customerId, hairstyleQuery, null, null, 5);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(match -> match.getMatchScore() > 0));
        assertTrue(result.stream().allMatch(match -> match.getMatchType() == Match.MatchType.HAIRSTYLE_QUERY));
        assertTrue(result.stream().allMatch(match -> match.getStatus() == Match.Status.POTENTIAL));
    }

    @Test
    void createDirectBooking_shouldCreateHighPriorityMatch() {
        // Arrange
        DirectBookingRequest request = DirectBookingRequest.builder()
                .customerId("customer-1")
                .stylistId("stylist-1")
                .serviceType("Haircut")
                .hairstyleName("Bob Cut")
                .notes("Direct booking test")
                .build();

        when(stylistRepository.findById("stylist-1")).thenReturn(Optional.of(stylist1));

        Match savedMatch = Match.builder()
                .id(1L)
                .customerId("customer-1")
                .stylistId("stylist-1")
                .matchScore(95.0)
                .matchType(Match.MatchType.DIRECT)
                .status(Match.Status.PENDING)
                .build();
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        doNothing().when(eventService).publishEvent(anyString(), anyString(), anyString(), any());

        // Act
        MatchResponse result = matchingService.createDirectBooking(request);

        // Assert
        assertEquals(95.0, result.getMatchScore());
        assertEquals(Match.MatchType.DIRECT, result.getMatchType());
        assertEquals(Match.Status.PENDING, result.getStatus());
        verify(matchRepository).save(any(Match.class));
        verify(eventService).publishEvent(anyString(), anyString(), anyString(), any());
    }
}
