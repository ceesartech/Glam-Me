package tech.ceesar.glamme.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShippingPurchaseResponse {
    private String shipmentId;

    private double rate;

    private String trackingNumber;

    private String labelUrl;
}
