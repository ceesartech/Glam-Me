package tech.ceesar.glamme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.ceesar.glamme.shopping.client.ShippingClient;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;
import tech.ceesar.glamme.shopping.service.ShippingService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class ShippingServiceTest {
    @Mock ShippingClient client;
    @InjectMocks ShippingService svc;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRates_returnsList() {
        ShippingRateRequest req = new ShippingRateRequest();
        when(client.getRates(req))
                .thenReturn(List.of(new ShippingRateResponse(
                        "shippingId1","UPS","Ground",10.0,"USD",3)));
        var list = svc.getRates(req);
        assertEquals(1, list.size());
    }
}
