package tech.ceesar.glamme.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateOrderResponse {
    private UUID orderId;

    private String checkoutUrl;
}
