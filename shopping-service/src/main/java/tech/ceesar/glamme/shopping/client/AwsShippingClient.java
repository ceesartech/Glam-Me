package tech.ceesar.glamme.shopping.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseRequest;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseResponse;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * AWS-based shipping client replacing EasyPost
 * Uses SES for email notifications and S3 for shipping document storage
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AwsShippingClient implements ShippingClient {

    private final S3Client s3Client;
    private final SesClient sesClient;

    @Override
    public List<ShippingRateResponse> getRates(ShippingRateRequest req) {
        // Mock shipping rates for demonstration
        // In production, integrate with actual shipping providers via AWS Lambda
        log.info("Getting shipping rates from {} to {}", req.getFrom().getCity(), req.getTo().getCity());

        return List.of(
                new ShippingRateResponse(
                        "standard-" + UUID.randomUUID().toString(),
                        "AWS_STANDARD",
                        "Ground",
                        9.99,
                        "USD",
                        5
                ),
                new ShippingRateResponse(
                        "express-" + UUID.randomUUID().toString(),
                        "AWS_EXPRESS",
                        "Express",
                        19.99,
                        "USD",
                        2
                ),
                new ShippingRateResponse(
                        "overnight-" + UUID.randomUUID().toString(),
                        "AWS_OVERNIGHT",
                        "Overnight",
                        29.99,
                        "USD",
                        1
                )
        );
    }

    @Override
    public ShippingPurchaseResponse purchase(ShippingPurchaseRequest req) {
        try {
            // Generate tracking information
            String trackingCode = "AWS" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

            // In a real implementation, you would:
            // 1. Call actual shipping provider via AWS Lambda
            // 2. Store shipping label in S3
            // 3. Send notification email via SES

            log.info("Purchasing shipment with rate: {}", req.getRateId());

            // Send shipping confirmation email
            sendShippingConfirmation(req, trackingCode);

            return new ShippingPurchaseResponse(
                    req.getShipmentId(),
                    9.99, // Mock cost
                    trackingCode,
                    "https://s3.amazonaws.com/shipping-labels/" + req.getShipmentId() + ".pdf"
            );

        } catch (Exception e) {
            log.error("Failed to purchase shipment", e);
            throw new RuntimeException("Failed to purchase shipment", e);
        }
    }

    private void sendShippingConfirmation(ShippingPurchaseRequest req, String trackingCode) {
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source("noreply@glamme.com")
                    .destination(d -> d.toAddresses("customer@example.com"))
                    .message(m -> m
                            .subject(s -> s.data("Your GlamMe Order Shipped!"))
                            .body(b -> b.text(t -> t.data(
                                    "Your order has been shipped!\n\n" +
                                    "Tracking Code: " + trackingCode + "\n" +
                                    "Track your package: https://glamme.com/track/" + trackingCode
                            )))
                    )
                    .build();

            sesClient.sendEmail(emailRequest);
            log.info("Shipping confirmation email sent for tracking: {}", trackingCode);

        } catch (Exception e) {
            log.warn("Failed to send shipping confirmation email", e);
        }
    }
}
