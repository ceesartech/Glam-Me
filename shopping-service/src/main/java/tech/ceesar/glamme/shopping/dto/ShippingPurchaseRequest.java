package tech.ceesar.glamme.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShippingPurchaseRequest {
    @NotBlank
    private String shipmentId;

    @NotBlank
    private String rateId;
}
