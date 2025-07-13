package tech.ceesar.glamme.matching.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.ceesar.glamme.common.enums.SubscriptionType;
import tech.ceesar.glamme.matching.dto.*;
import tech.ceesar.glamme.matching.entity.AddOn;
import tech.ceesar.glamme.matching.entity.ServiceOffering;
import tech.ceesar.glamme.matching.entity.StylistProfile;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchingServiceTest {
    @Mock ServiceOfferingRepository offeringRepository;
    @Mock StylistRepository stylistRepository;
    @InjectMocks MatchingService matchingService;

    private StylistProfile profile1, profile2;
    private ServiceOffering off1, off2;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        // Setting up summy stylists
        profile1 = StylistProfile.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .specialties(Set.of("boho braids", "k-tips", "fades"))
                .eloRating(1600)
                .latitude(40.0)
                .longitude(-105.0)
                .build();

        profile2 = StylistProfile.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .specialties(Set.of("knotless braids","tribal braids", "twists", "fades"))
                .eloRating(1700)
                .latitude(40.1)
                .longitude(-105.1)
                .build();

        // Offering 1: lower Elo, cheaper, no add-ons
        off1 = ServiceOffering.builder()
                .id(UUID.randomUUID())
                .stylistProfile(profile1)
                .styleName("bob")
                .costPerHour(50)
                .estimatedHours(1.0)
                .build();

        // Offering 2: higher Elo, more expensive, one add-on
        off2 = ServiceOffering.builder()
                .id(UUID.randomUUID())
                .stylistProfile(profile2)
                .styleName("bob")
                .costPerHour(70)
                .estimatedHours(1.0)
                .build();
        off2.setAddOns(List.of(
                AddOn.builder().name("deep-condition").cost(20).offering(off2).build()
        ));
    }

    @Test
    void recommendOfferings_sortsByEloThenCostThenDistance() {
        // Given two offerings for "bob"
        when(offeringRepository.findByStyleName("bob")).thenReturn(List.of(off1, off2));

        List<OfferingResponse> result = matchingService.recommendOfferings(
                "bob", 40.0, -105.0, 10);

        // off2 has higher Elo → should come first
        assertEquals(2, result.size());
        assertEquals(off2.getId(), result.get(0).getOfferingId());
        assertEquals(off1.getId(), result.get(1).getOfferingId());

        // Verify totalCost computation for off2: 70*1 + 20 = 90
        OfferingResponse resp2 = result.get(0);
        assertEquals(90.0, resp2.getTotalCost(), 0.001);

        // Verify totalCost for off1: 50*1 + 0 = 50
        OfferingResponse resp1 = result.get(1);
        assertEquals(50.0, resp1.getTotalCost(), 0.001);
    }

    @Test
    void recommendOfferings_fallbackToAllIfNoMatch() {
        when(offeringRepository.findByStyleName("weave")).thenReturn(Collections.emptyList());
        when(offeringRepository.findAll()).thenReturn(List.of(off1, off2));

        List<OfferingResponse> result = matchingService.recommendOfferings(
                "weave", 40.0, -105.0, 1);

        // Limit = 1 → only the top offering (off2) should be returned
        assertEquals(1, result.size());
        assertEquals(off2.getId(), result.get(0).getOfferingId());
    }

    @Test
    void updateElo_computation() {
        double rA = matchingService.updateElo(1600, 1700, true);
        // winner of lower rating gains: expect >1600
        assertTrue(rA > 1600);

        double rB = matchingService.updateElo(1700, 1600, false);
        assertTrue(rB < 1700);
    }

    @Test
    void stableMatch_simple() {
        // Two customers: c1(premium), c2(free)
        CustomerDto customer1 = new CustomerDto();
        customer1.setId(UUID.fromString("bb299620-6163-4b1e-aa5a-fecd3c4a85b6"));
        customer1.setSubscriptionType(SubscriptionType.FREE);
        CustomerDto customer2 = new CustomerDto();
        customer2.setId(UUID.fromString("fb0bc32c-da45-411c-ad7e-4010c1133c4b"));
        customer2.setSubscriptionType(SubscriptionType.PREMIUM);

        // One stylist
        StylistDto s = StylistDto.builder()
                .id(UUID.randomUUID())
                .eloRating(1600)
                .costPerHour(50)
                .build();

        StableMatchRequest req = new StableMatchRequest();
        req.setCustomers(List.of(customer1, customer2));
        req.setStylists(List.of(s));

        List<PairDto> pairs = matchingService.stableMatch(req);
        // Only one match: stylist → premium customer
        assertEquals(1, pairs.size());
        assertEquals(customer2.getId(), pairs.get(0).getCustomerId());
    }
}
