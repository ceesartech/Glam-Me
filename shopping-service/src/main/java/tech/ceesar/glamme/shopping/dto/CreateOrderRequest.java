package tech.ceesar.glamme.shopping.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.ceesar.glamme.shopping.enums.ShippingOption;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    @NotNull
    private UUID customerId;

    @NotEmpty
    private List<OrderItemDto> items;

    @NotNull
    private ShippingOption shippingOption;

    private AddressDto shippingAddress; // required if CLIENT

    @NotNull
    private ShippingRateResponse selectedRate;
}
