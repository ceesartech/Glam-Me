package tech.ceesar.glamme.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import tech.ceesar.glamme.shopping.enums.OrderStatus;
import tech.ceesar.glamme.shopping.enums.PaymentStatus;
import tech.ceesar.glamme.shopping.enums.ShippingOption;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OrderResponse {
    private UUID orderResponseId;

    private double productTotal;

    private double shippingCost;

    private double totalAmount;

    private PaymentStatus paymentStatus;

    private OrderStatus orderStatus;

    private ShippingOption shippingOption;

    private String shippingCarrier;

    private String shippingService;

    private String shippingTracking;

    private String shippingLabelUrl;

    private AddressDto shippingAddress;

    private Instant createdAt;
}
