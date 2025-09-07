package tech.ceesar.glamme.shopping.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.event.EventPublisher;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final OrderService orderService;
    private final EventPublisher eventPublisher;

    @Value("${stripe.webhookSecret:}")
    private String webhookSecret;

    /**
     * Process Stripe webhook events
     */
    public void processWebhookEvent(String payload, String signature) throws SignatureVerificationException {
        // Verify webhook signature
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            Webhook.constructEvent(payload, signature, webhookSecret);
        }

        try {
            // Parse webhook payload (simplified for now)
            log.info("Processing Stripe webhook payload: {}", payload.substring(0, Math.min(100, payload.length())));
            // TODO: Implement proper Stripe event parsing based on SDK version
            return;
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getData().getObject();

        try {
            // Extract order ID from session metadata or client_reference_id
            String orderIdStr = session.getClientReferenceId();
            if (orderIdStr == null) {
                log.warn("No client reference ID found in session: {}", session.getId());
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);

            // Handle successful payment
            orderService.handlePaymentSucceeded(orderId);

            log.info("Checkout session completed for order: {}", orderId);

        } catch (Exception e) {
            log.error("Error handling checkout session completed", e);
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) event.getData().getObject();

        // Publish payment succeeded event
        eventPublisher.publishEvent("stripe.payment_intent.succeeded", Map.of(
                "paymentIntentId", paymentIntent.getId(),
                "amount", paymentIntent.getAmount(),
                "currency", paymentIntent.getCurrency(),
                "customerId", paymentIntent.getCustomer()
        ));

        log.info("Payment intent succeeded: {}", paymentIntent.getId());
    }

    private void handlePaymentIntentFailed(Event event) {
        com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) event.getData().getObject();

        String failureReason = paymentIntent.getLastPaymentError() != null
                ? paymentIntent.getLastPaymentError().getMessage()
                : "Unknown payment failure";

        // Try to extract order ID from metadata
        String orderIdStr = paymentIntent.getMetadata() != null
                ? paymentIntent.getMetadata().get("orderId")
                : null;

        if (orderIdStr != null) {
            try {
                UUID orderId = UUID.fromString(orderIdStr);
                orderService.handlePaymentFailed(orderId, failureReason);
            } catch (Exception e) {
                log.error("Error handling payment failure for order: {}", orderIdStr, e);
            }
        }

        // Publish payment failed event
        eventPublisher.publishEvent("stripe.payment_intent.failed", Map.of(
                "paymentIntentId", paymentIntent.getId(),
                "amount", paymentIntent.getAmount(),
                "currency", paymentIntent.getCurrency(),
                "failureReason", failureReason
        ));

        log.info("Payment intent failed: {} - {}", paymentIntent.getId(), failureReason);
    }

    private void handleChargeDispute(Event event) {
        com.stripe.model.Dispute dispute = (com.stripe.model.Dispute) event.getData().getObject();

        // Publish dispute created event
        eventPublisher.publishEvent("stripe.dispute.created", Map.of(
                "disputeId", dispute.getId(),
                "chargeId", dispute.getCharge(),
                "amount", dispute.getAmount(),
                "currency", dispute.getCurrency(),
                "reason", dispute.getReason(),
                "status", dispute.getStatus()
        ));

        log.info("Charge dispute created: {} for charge: {}", dispute.getId(), dispute.getCharge());
    }

    /**
     * Validate webhook signature (for additional security)
     */
    public boolean isValidWebhookSignature(String payload, String signature, String endpointSecret) {
        try {
            Webhook.constructEvent(payload, signature, endpointSecret);
            return true;
        } catch (SignatureVerificationException e) {
            log.warn("Invalid webhook signature", e);
            return false;
        }
    }
}
