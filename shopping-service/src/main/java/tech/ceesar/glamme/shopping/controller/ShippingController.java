package tech.ceesar.glamme.shopping.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseRequest;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseResponse;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;
import tech.ceesar.glamme.shopping.service.ShippingService;

import java.util.List;

@RestController
@RequestMapping("/api/shopping/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService shippingService;

    @PostMapping("/rates")
    public List<ShippingRateResponse> rates(
            @RequestBody ShippingRateRequest req) {
        return shippingService.getRates(req);
    }

    @PostMapping("/purchase")
    public ShippingPurchaseResponse buy(
            @RequestBody ShippingPurchaseRequest req) {
        return shippingService.purchase(req);
    }
}
