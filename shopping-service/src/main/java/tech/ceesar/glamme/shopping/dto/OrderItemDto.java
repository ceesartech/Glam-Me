package tech.ceesar.glamme.shopping.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class OrderItemDto {
    @NotNull
    private UUID productId;

    @Positive
    private int quantity;
}
