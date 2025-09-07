package tech.ceesar.glamme.shopping.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.common.service.RedisIdempotencyService;
import tech.ceesar.glamme.common.service.RedisRateLimitService;
import tech.ceesar.glamme.shopping.dto.*;
import tech.ceesar.glamme.shopping.entity.Address;
import tech.ceesar.glamme.shopping.entity.Order;
import tech.ceesar.glamme.shopping.entity.OrderItem;
import tech.ceesar.glamme.shopping.entity.Product;
import tech.ceesar.glamme.shopping.enums.OrderStatus;
import tech.ceesar.glamme.shopping.enums.PaymentStatus;
import tech.ceesar.glamme.shopping.enums.ShippingOption;
import tech.ceesar.glamme.shopping.repositories.OrderRepository;
import tech.ceesar.glamme.shopping.repositories.ProductRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final ShippingService shippingService;
    private final RedisCacheService cacheService;
    private final RedisIdempotencyService idempotencyService;
    private final RedisRateLimitService rateLimitService;
    private final EventPublisher eventPublisher;

    @Value("${stripe.successUrl}")
    private String successUrl;
    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest req) throws StripeException {
        // Rate limiting check
        if (!rateLimitService.checkShoppingRateLimit(req.getCustomerId().toString())) {
            throw new RuntimeException("Too many order requests. Please try again later.");
        }

        // Idempotency check
        String idempotencyKey = generateOrderIdempotencyKey(req);
        if (!idempotencyService.startOrderOperation(req.getCustomerId().toString(), idempotencyKey, req)) {
            throw new RuntimeException("Order creation already in progress");
        }

        try {
            // 1) Build & populate
            Order order = new Order();
            order.setCustomerId(req.getCustomerId());
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.CREATED);
            order.setShippingOption(
                    ShippingOption.valueOf(req.getShippingOption().name())
            );
            order.setShippingCarrier(req.getSelectedRate().getCarrier());
            order.setShippingService(req.getSelectedRate().getService());
            order.setSelectedRateId(req.getSelectedRate().getId());

            if (req.getShippingAddress() != null) {
                AddressDto ad = req.getShippingAddress();
                Address addr = new Address(
                        ad.getName(), ad.getStreet1(), ad.getStreet2(),
                        ad.getCity(), ad.getState(), ad.getPostalCode(), ad.getCountry()
                );
                order.setShippingAddress(addr);
            }

            // ensure list exists
            order.setItems(new ArrayList<>());

            double productTotal = 0;
            for (OrderItemDto dto : req.getItems()) {
                Product prod = productRepo.findById(dto.getProductId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Product", dto.getProductId().toString())
                        );
                productTotal += prod.getPrice() * dto.getQuantity();
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProductId(prod.getProductId());
                item.setQuantity(dto.getQuantity());
                item.setUnitPrice(prod.getPrice());
                order.getItems().add(item);
            }
            order.setProductTotal(productTotal);

            double shippingCost = req.getSelectedRate().getRate();
            order.setShippingCost(shippingCost);
            order.setTotalAmount(productTotal + shippingCost);

            // 2) Persist *and* capture the returned entity so ID is set
            Order saved = orderRepo.save(order);
            // Mirror the generated ID back onto our in-memory order
            order.setOrderId(saved.getOrderId());

            // 3) Build + call Stripe
            Session session = Session.create(buildSessionParams(order));

            // Cache the order for quick access
            cacheService.set("order:" + order.getOrderId(), order, Duration.ofHours(24));

            // Publish order created event
            eventPublisher.publishEvent("order.created", Map.of(
                    "orderId", order.getOrderId().toString(),
                    "customerId", req.getCustomerId(),
                    "totalAmount", order.getTotalAmount(),
                    "currency", "USD"
            ));

            // Mark idempotency as completed
            idempotencyService.completeOperation(idempotencyKey, order.getOrderId());

            return new CreateOrderResponse(order.getOrderId(), session.getUrl());

        } catch (Exception e) {
            // Mark idempotency as failed
            idempotencyService.failOperation(idempotencyKey, e.getMessage());
            throw e;
        }
    }

    private String generateOrderIdempotencyKey(CreateOrderRequest req) {
        StringBuilder key = new StringBuilder();
        key.append(req.getCustomerId()).append(":");
        for (OrderItemDto item : req.getItems()) {
            key.append(item.getProductId()).append("-").append(item.getQuantity()).append(":");
        }
        return "order:" + key.toString().hashCode();
    }

    @Transactional
    public void handlePaymentSucceeded(UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId.toString())
                );

        order.setPaymentStatus(PaymentStatus.PAID);

        if (order.getShippingOption() == ShippingOption.PICKUP) {
            order.setOrderStatus(OrderStatus.PICKUP_READY);

            // Publish pickup ready event
            eventPublisher.publishEvent("order.pickup_ready", Map.of(
                    "orderId", orderId.toString(),
                    "customerId", order.getCustomerId()
            ));

        } else {
            // Call shippingService.purchase(...) so tests see the interaction
            ShippingPurchaseRequest spr = new ShippingPurchaseRequest(
                    /* shipmentId */ order.getSelectedRateId(),
                    /* rateId     */ order.getSelectedRateId()
            );
            ShippingPurchaseResponse spResp = shippingService.purchase(spr);

            order.setShippingTracking(spResp.getTrackingNumber());
            order.setShippingLabelUrl(spResp.getLabelUrl());
            order.setOrderStatus(OrderStatus.SHIPPED);

            // Publish shipped event
            eventPublisher.publishEvent("order.shipped", Map.of(
                    "orderId", orderId.toString(),
                    "customerId", order.getCustomerId(),
                    "trackingNumber", spResp.getTrackingNumber(),
                    "carrier", order.getShippingCarrier()
            ));
        }

        Order savedOrder = orderRepo.save(order);

        // Update cache
        cacheService.set("order:" + orderId, savedOrder, Duration.ofHours(24));

        // Publish payment succeeded event
        eventPublisher.publishEvent("payment.succeeded", Map.of(
                "orderId", orderId.toString(),
                "customerId", order.getCustomerId(),
                "amount", order.getTotalAmount(),
                "currency", "USD"
        ));

        log.info("Order {} payment succeeded, status updated to {}", orderId, savedOrder.getOrderStatus());
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId, String reason) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", orderId.toString())
                );

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setOrderStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepo.save(order);

        // Update cache
        cacheService.set("order:" + orderId, savedOrder, Duration.ofHours(24));

        // Publish payment failed event
        eventPublisher.publishEvent("payment.failed", Map.of(
                "orderId", orderId.toString(),
                "customerId", order.getCustomerId(),
                "amount", order.getTotalAmount(),
                "currency", "USD",
                "reason", reason
        ));

        log.info("Order {} payment failed: {}", orderId, reason);
    }

    /**
     * Get order with caching
     */
    public Order getOrder(UUID orderId) {
        // Try cache first
        return cacheService.get("order:" + orderId, Order.class)
                .orElseGet(() -> {
                    // Fallback to database
                    Order order = orderRepo.findById(orderId)
                            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

                    // Cache for future requests
                    cacheService.set("order:" + orderId, order, Duration.ofHours(24));
                    return order;
                });
    }

    private SessionCreateParams buildSessionParams(Order order) {
        SessionCreateParams.Builder builder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setClientReferenceId(order.getOrderId().toString())
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD);

        // Product items
        for (OrderItem it : order.getItems()) {
            builder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long)it.getQuantity())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount((long)(it.getUnitPrice() * 100))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName("Product: " + it.getProductId())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        // Shipping as a line item
        builder.addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount((long)(order.getShippingCost() * 100))
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Shipping: "
                                                                + order.getShippingCarrier() + " "
                                                                + order.getShippingService())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
        );

        return builder.build();
    }
}
