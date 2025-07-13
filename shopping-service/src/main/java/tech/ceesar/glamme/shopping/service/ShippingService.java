package tech.ceesar.glamme.shopping.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.shopping.client.ShippingClient;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseRequest;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseResponse;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingService {
    private final ShippingClient client;

    public List<ShippingRateResponse> getRates(ShippingRateRequest req) {
        return client.getRates(req);
    }

    public ShippingPurchaseResponse purchase(ShippingPurchaseRequest req) {
        return client.purchase(req);
    }
}
