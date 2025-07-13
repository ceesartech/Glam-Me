package tech.ceesar.glamme.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProductResponse {
    private UUID id;

    private String name;

    private String description;

    private double price;

    private double weight;

    private String sku;

    private String imageUrl;
}
