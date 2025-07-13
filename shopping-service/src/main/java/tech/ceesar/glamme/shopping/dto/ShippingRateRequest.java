package tech.ceesar.glamme.shopping.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ShippingRateRequest {
    @NotNull
    private AddressDto from;

    @NotNull
    private AddressDto to;

    @Positive
    private double weight;     // grams

    @Positive
    private double length;     // cm

    @Positive
    private double width;      // cm

    @Positive
    private double height;     // cm
}
