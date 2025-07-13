package tech.ceesar.glamme.shopping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tech.ceesar.glamme.shopping.enums.OrderStatus;
import tech.ceesar.glamme.shopping.enums.PaymentStatus;
import tech.ceesar.glamme.shopping.enums.ShippingOption;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue
    private UUID orderId;

    @Column(nullable = false)
    private UUID customerId;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private double productTotal;

    @Column(nullable = false)
    private double shippingCost;

    @Column(nullable = false)
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // PENDING, PAID

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;     // CREATED, SHIPPED, DELIVERED, PICKUP_READY

    @Enumerated(EnumType.STRING)
    private ShippingOption shippingOption; // CLIENT, STYLIST, PICKUP

    // **NEW**: the EasyPost rate ID the user selected
    @Column(nullable = true)
    private String selectedRateId;

    // shipping details
    @Column(nullable = true)
    private String shippingCarrier;

    @Column(nullable = true)
    private String shippingService;

    @Column(nullable = true)
    private String shippingTracking;

    @Column(nullable = true)
    private String shippingLabelUrl;

    @Embedded
    private Address shippingAddress;

    @CreationTimestamp
    private Instant createdAt;
}
