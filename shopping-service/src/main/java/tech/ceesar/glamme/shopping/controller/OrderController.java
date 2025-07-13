package tech.ceesar.glamme.shopping.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.shopping.dto.CreateOrderRequest;
import tech.ceesar.glamme.shopping.dto.CreateOrderResponse;
import tech.ceesar.glamme.shopping.dto.OrderResponse;
import tech.ceesar.glamme.shopping.service.OrderQueryService;
import tech.ceesar.glamme.shopping.service.OrderService;

import java.io.BufferedReader;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shopping/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderQueryService queryService;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    /**
     * Create a new order and return a Stripe Checkout URL.
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest req
    ) throws StripeException {
        CreateOrderResponse resp = orderService.createOrder(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resp);
    }

    /**
     * Retrieve a single order’s details.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId
    ) {
        OrderResponse resp = queryService.getOrderById(orderId);
        return ResponseEntity.ok(resp);
    }

    /**
     * List orders, optionally filtered by customerId.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(required = false) UUID customerId
    ) {
        List<OrderResponse> resp = (customerId == null)
                ? queryService.listAllOrders()
                : queryService.listOrdersByCustomer(customerId);
        return ResponseEntity.ok(resp);
    }

    /**
     * Stripe webhook to handle payment events.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) throws Exception {
        // 1) Read raw body
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            reader.lines().forEach(payload::append);
        }

        // 2) Verify signature & parse event
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload.toString(), sigHeader, webhookSecret
            );
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 3) Handle checkout.session.completed
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();
            UUID orderId = UUID.fromString(session.getClientReferenceId());
            orderService.handlePaymentSucceeded(orderId);
        }

        return ResponseEntity.ok().build();
    }
}
