package tech.ceesar.glamme.booking.controller;

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
import tech.ceesar.glamme.booking.dto.CreateBookingRequest;
import tech.ceesar.glamme.booking.dto.CreateBookingResponse;
import tech.ceesar.glamme.booking.service.BookingService;

import java.io.BufferedReader;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<CreateBookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest bookingRequest
    ) throws StripeException {
        var response = bookingService.createBooking(bookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable("id") UUID bookingId
    ) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    // Complete
    @PutMapping("/{id}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable("id") UUID bookingId
    ) {
        bookingService.completeBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) throws Exception {
        // read raw body
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            reader.lines().forEach(stringBuilder::append);
        }
        String payload = stringBuilder.toString();

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();
            UUID bookingId = UUID.fromString(session.getClientReferenceId());
            bookingService.handlePaymentSucceeded(bookingId);
        }
        return ResponseEntity.ok().build();
    }
}
