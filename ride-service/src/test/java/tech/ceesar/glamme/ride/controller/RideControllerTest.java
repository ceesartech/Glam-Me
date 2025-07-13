package tech.ceesar.glamme.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.service.RideService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
class RideControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean
    RideService svc;

    @Test
    void requestRide_returns201() throws Exception {
        CreateRideRequest req = new CreateRideRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setProviderType(ProviderType.INTERNAL);
        LocationDto loc = new LocationDto(); loc.setLatitude(0); loc.setLongitude(0);
        req.setPickupLocation(loc); req.setDropoffLocation(loc);

        CreateRideResponse resp = new CreateRideResponse(UUID.randomUUID(), null);
        when(svc.createRide(any())).thenReturn(resp);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value(resp.getRideId().toString()));
    }

    @Test
    void getStatus_returns200() throws Exception {
        UUID rid = UUID.randomUUID();
        RideStatusResponse resp = new RideStatusResponse(
                rid, ProviderType.UBER, RideStatus.REQUESTED, "E1", null
        );
        when(svc.getStatus(rid)).thenReturn(resp);

        mockMvc.perform(get("/api/rides/{rideId}/status", rid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void cancelRide_returnsTrue() throws Exception {
        UUID rid = UUID.randomUUID();
        CancelRideResponse resp = new CancelRideResponse(true);
        when(svc.cancelRide(rid)).thenReturn(resp);

        mockMvc.perform(post("/api/rides/{rideId}/cancel", rid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(true));
    }
}