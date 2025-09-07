package tech.ceesar.glamme.shopping.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.shopping.service.StripeWebhookService;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    /**
     * Handle Stripe webhooks
     */
    @PostMapping("/webhooks")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        try {
            stripeWebhookService.processWebhookEvent(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Webhook processing failed: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for webhook monitoring
     */
    @GetMapping("/webhooks/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Stripe webhook service is healthy");
    }
}
