package tech.ceesar.glamme.shopping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue
    private UUID OrderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    private Order order;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double unitPrice;
}
