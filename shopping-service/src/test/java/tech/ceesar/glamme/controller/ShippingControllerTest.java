package tech.ceesar.glamme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import tech.ceesar.glamme.shopping.ShoppingServiceApplication;
import tech.ceesar.glamme.shopping.controller.ShippingController;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;
import tech.ceesar.glamme.shopping.service.ShippingService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShippingController.class)
// Tell Spring Boot to use your main config class
@ContextConfiguration(classes = ShoppingServiceApplication.class)
class ShippingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean
    ShippingService svc;

    @Test
    void ratesEndpoint_returnsJsonWithId() throws Exception {
        ShippingRateRequest req = new ShippingRateRequest();
        ShippingRateResponse resp = new ShippingRateResponse(
                "RATE1", "FedEx", "Overnight", 20.0, "USD", 1
        );
        when(svc.getRates(any())).thenReturn(List.of(resp));

        mockMvc.perform(post("/api/shopping/shipping/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("RATE1"))
                .andExpect(jsonPath("$[0].carrier").value("FedEx"));
    }
}
