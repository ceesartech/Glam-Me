package tech.ceesar.glamme.shopping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue
    private UUID productId;

    @Column(nullable = false)
    private UUID sellerId;     // stylist userId or null for platform

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private double price;      // USD

    @Column(nullable = false)
    private double weight;     // grams (for shipping)

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String imageUrl;   // S3 URL
}
