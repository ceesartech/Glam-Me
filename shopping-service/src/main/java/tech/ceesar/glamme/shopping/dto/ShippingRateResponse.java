package tech.ceesar.glamme.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShippingRateResponse {
    private String id;

    private String carrier;

    private String service;

    private double rate;

    private String currency;

    private Integer estimatedDays;
}
