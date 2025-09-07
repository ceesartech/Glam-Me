package tech.ceesar.glamme.booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.booking.dto.PaymentRequest;
import tech.ceesar.glamme.booking.dto.PaymentResponse;
import tech.ceesar.glamme.booking.dto.RefundRequest;
import tech.ceesar.glamme.booking.dto.RefundResponse;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentResponse createPaymentIntent(PaymentRequest request) {
        try {
            log.info("Creating payment intent for amount: {} for customer: {}", 
                    request.getAmount(), request.getCustomerId());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(convertToStripeAmount(request.getAmount()))
                    .setCurrency("usd")
                    .setCustomer(request.getCustomerId())
                    .setDescription(request.getDescription())
                    .putMetadata("booking_id", request.getBookingId())
                    .putMetadata("stylist_id", request.getStylistId())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                    .setConfirm(true)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            return PaymentResponse.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .status(paymentIntent.getStatus())
                    .amount(request.getAmount())
                    .currency("usd")
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    public PaymentResponse confirmPaymentIntent(String paymentIntentId) {
        try {
            log.info("Confirming payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent confirmedPaymentIntent = paymentIntent.confirm();

            return PaymentResponse.builder()
                    .paymentIntentId(confirmedPaymentIntent.getId())
                    .status(confirmedPaymentIntent.getStatus())
                    .amount(convertFromStripeAmount(confirmedPaymentIntent.getAmount()))
                    .currency(confirmedPaymentIntent.getCurrency())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to confirm payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Payment confirmation failed: " + e.getMessage(), e);
        }
    }

    public PaymentResponse cancelPaymentIntent(String paymentIntentId) {
        try {
            log.info("Cancelling payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent cancelledPaymentIntent = paymentIntent.cancel();

            return PaymentResponse.builder()
                    .paymentIntentId(cancelledPaymentIntent.getId())
                    .status(cancelledPaymentIntent.getStatus())
                    .amount(convertFromStripeAmount(cancelledPaymentIntent.getAmount()))
                    .currency(cancelledPaymentIntent.getCurrency())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to cancel payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Payment cancellation failed: " + e.getMessage(), e);
        }
    }

    public RefundResponse createRefund(RefundRequest request) {
        try {
            log.info("Creating refund for payment intent: {} amount: {}", 
                    request.getPaymentIntentId(), request.getAmount());

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(request.getPaymentIntentId())
                    .setAmount(convertToStripeAmount(request.getAmount()))
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .putMetadata("booking_id", request.getBookingId())
                    .putMetadata("reason", request.getReason())
                    .build();

            Refund refund = Refund.create(params);

            return RefundResponse.builder()
                    .refundId(refund.getId())
                    .paymentIntentId(refund.getPaymentIntent())
                    .amount(convertFromStripeAmount(refund.getAmount()))
                    .status(refund.getStatus())
                    .reason(refund.getReason())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create refund: {}", e.getMessage(), e);
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }
    }

    public PaymentResponse getPaymentIntent(String paymentIntentId) {
        try {
            log.info("Retrieving payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            return PaymentResponse.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .amount(convertFromStripeAmount(paymentIntent.getAmount()))
                    .currency(paymentIntent.getCurrency())
                    .clientSecret(paymentIntent.getClientSecret())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to retrieve payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Payment retrieval failed: " + e.getMessage(), e);
        }
    }

    public String createCustomer(String email, String name) {
        try {
            log.info("Creating Stripe customer for email: {}", email);

            Map<String, Object> params = new HashMap<>();
            params.put("email", email);
            params.put("name", name);

            com.stripe.model.Customer customer = com.stripe.model.Customer.create(params);

            log.info("Created Stripe customer: {}", customer.getId());
            return customer.getId();

        } catch (StripeException e) {
            log.error("Failed to create Stripe customer: {}", e.getMessage(), e);
            throw new RuntimeException("Customer creation failed: " + e.getMessage(), e);
        }
    }

    private long convertToStripeAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private BigDecimal convertFromStripeAmount(long amount) {
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100));
    }
}
