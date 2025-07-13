package tech.ceesar.glamme.shopping.client;

import tech.ceesar.glamme.shopping.dto.ShippingPurchaseRequest;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseResponse;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;

import java.util.List;

public interface ShippingClient {
    List<ShippingRateResponse> getRates(ShippingRateRequest req);
    ShippingPurchaseResponse purchase(ShippingPurchaseRequest req);
}
