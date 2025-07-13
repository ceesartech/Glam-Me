package tech.ceesar.glamme.matching.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tech.ceesar.glamme.matching.dto.AddOnDto;
import tech.ceesar.glamme.matching.dto.OfferingResponse;
import tech.ceesar.glamme.matching.service.MatchingService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchingController.class)
public class MatchingControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    @MockBean
    MatchingService matchingService;

    private List<OfferingResponse> offerings;

    @BeforeEach
    void setUp() {
        // three offerings with varying totalCost
        offerings = List.of(
                OfferingResponse.builder()
                        .offeringId(UUID.randomUUID())
                        .stylistProfileId(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .specialties(Set.of("bob"))
                        .eloRating(1500)
                        .distance(5.0)
                        .styleName("bob")
                        .costPerHour(50)
                        .estimatedHours(1.0)
                        .addOns(List.of(new AddOnDto("extra",10.0)))
                        .build(), // totalCost = 60
                OfferingResponse.builder()
                        .offeringId(UUID.randomUUID())
                        .stylistProfileId(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .specialties(Set.of("bob"))
                        .eloRating(1600)
                        .distance(3.0)
                        .styleName("bob")
                        .costPerHour(70)
                        .estimatedHours(1.0)
                        .addOns(List.of())
                        .build(), // totalCost = 70
                OfferingResponse.builder()
                        .offeringId(UUID.randomUUID())
                        .stylistProfileId(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .specialties(Set.of("bob"))
                        .eloRating(1400)
                        .distance(2.0)
                        .styleName("bob")
                        .costPerHour(40)
                        .estimatedHours(2.0)
                        .addOns(List.of())
                        .build()  // totalCost = 80
        );
        // stub service to ignore filtering/pagination logic
        when(matchingService.recommendOfferings(
                anyString(), anyDouble(), anyDouble(), anyInt()
        )).thenReturn(offerings);
    }

    @Test
    void recommend_default_noFilters_firstPage() throws Exception {
        mockMvc.perform(get("/api/match/recommend")
                        .param("styleName", "bob")
                        .param("latitude", "40.0")
                        .param("longitude", "-105.0")
                )
                .andExpect(status().isOk())
                // pagedResponse structure
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].styleName").value("bob"));
    }

    @Test
    void recommend_filterByMinCost_excludesBelow() throws Exception {
        mockMvc.perform(get("/api/match/recommend")
                        .param("styleName", "bob")
                        .param("latitude", "40.0")
                        .param("longitude", "-105.0")
                        .param("minCost", "65")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].totalCost")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.greaterThanOrEqualTo(65.0))));
    }

    @Test
    void recommend_pagination_worksCorrectly() throws Exception {
        mockMvc.perform(get("/api/match/recommend")
                        .param("styleName", "bob")
                        .param("latitude", "40.0")
                        .param("longitude", "-105.0")
                        .param("page", "1")
                        .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
