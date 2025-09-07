package tech.ceesar.glamme.booking.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.repository.BookingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;

    @Value("${stripe.success-url:http://localhost:8080/payment/success}")
    private String successUrl;

    @Value("${stripe.cancel-url:http://localhost:8080/payment/cancel}")
    private String cancelUrl;

    /**
     * Create Stripe checkout session for booking payment
     */
    public String createCheckoutSession(String bookingId) throws StripeException {
        log.info("Creating checkout session for booking: {}", bookingId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found: " + bookingId);
        }

        // Convert price to cents
        long amount = booking.getPrice().multiply(new java.math.BigDecimal(100)).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(bookingId)
                .setSuccessUrl(successUrl + "?bookingId=" + bookingId)
                .setCancelUrl(cancelUrl + "?bookingId=" + bookingId)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(booking.getServiceName())
                                                                .setDescription(booking.getServiceDescription())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        log.info("Created checkout session: {} for booking: {}", session.getId(), bookingId);

        return session.getUrl();
    }

    /**
     * Process successful payment
     */
    public void processPaymentSuccess(String bookingId, String paymentIntentId) {
        log.info("Processing payment success for booking: {} with payment intent: {}", bookingId, paymentIntentId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found: " + bookingId);
        }

        booking.setPaymentIntentId(paymentIntentId);
        booking.setPaymentStatus(Booking.PaymentStatus.CAPTURED);
        bookingRepository.save(booking);

        log.info("Payment processed successfully for booking: {}", bookingId);
    }

    /**
     * Process failed payment
     */
    public void processPaymentFailure(String bookingId) {
        log.info("Processing payment failure for booking: {}", bookingId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found: " + bookingId);
        }

        booking.setPaymentStatus(Booking.PaymentStatus.FAILED);
        bookingRepository.save(booking);

        log.info("Payment failure processed for booking: {}", bookingId);
    }

    /**
     * Create refund for booking
     */
    public String createRefund(String bookingId) throws StripeException {
        log.info("Creating refund for booking: {}", bookingId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found: " + bookingId);
        }

        if (booking.getPaymentIntentId() == null) {
            throw new RuntimeException("No payment found for booking: " + bookingId);
        }

        // In a real implementation, you would use Stripe's refund API
        // For now, we'll just mark the payment as refunded
        booking.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        log.info("Refund processed for booking: {}", bookingId);
        return "refund-" + System.currentTimeMillis();
    }
}
