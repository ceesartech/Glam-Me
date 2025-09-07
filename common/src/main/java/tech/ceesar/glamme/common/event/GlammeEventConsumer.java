package tech.ceesar.glamme.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.ceesar.glamme.common.service.EventBridgeConsumerService;

@Component
@Slf4j
public class GlammeEventConsumer extends BaseEventHandler {

    public GlammeEventConsumer(EventBridgeConsumerService eventBridgeConsumer) {
        super(eventBridgeConsumer);
    }

    @Override
    public void registerHandlers() {
        // Register user event handlers
        eventBridgeConsumer.registerHandler("user.registered", this::handleUserEvent);
        eventBridgeConsumer.registerHandler("user.profile.updated", this::handleUserEvent);
        eventBridgeConsumer.registerHandler("user.deleted", this::handleUserEvent);

        // Register booking event handlers
        eventBridgeConsumer.registerHandler("booking.created", this::handleBookingEvent);
        eventBridgeConsumer.registerHandler("booking.confirmed", this::handleBookingEvent);
        eventBridgeConsumer.registerHandler("booking.cancelled", this::handleBookingEvent);
        eventBridgeConsumer.registerHandler("booking.completed", this::handleBookingEvent);

        // Register payment event handlers
        eventBridgeConsumer.registerHandler("payment.succeeded", this::handlePaymentEvent);
        eventBridgeConsumer.registerHandler("payment.failed", this::handlePaymentEvent);
        eventBridgeConsumer.registerHandler("payment.refunded", this::handlePaymentEvent);

        // Register image event handlers
        eventBridgeConsumer.registerHandler("image.job.completed", this::handleImageEvent);
        eventBridgeConsumer.registerHandler("image.job.failed", this::handleImageEvent);

        log.info("Registered all GlamMe event handlers");
    }

    @Override
    protected void onUserRegistered(String userId, EventBridgeConsumerService.EventMessage event) {
        // Handle user registration
        log.info("Processing user registration for user: {}", userId);

        // Example: Send welcome email, create user profile, etc.
        // This would be implemented by the specific service
    }

    @Override
    protected void onUserProfileUpdated(String userId, EventBridgeConsumerService.EventMessage event) {
        // Handle user profile update
        log.info("Processing user profile update for user: {}", userId);

        // Example: Update search indices, invalidate caches, etc.
    }

    @Override
    protected void onUserDeleted(String userId, EventBridgeConsumerService.EventMessage event) {
        // Handle user deletion
        log.info("Processing user deletion for user: {}", userId);

        // Example: Clean up user data, cancel bookings, etc.
    }

    @Override
    protected void onBookingCreated(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event) {
        // Handle booking creation
        log.info("Processing booking creation: {} for user: {} with stylist: {}", bookingId, userId, stylistId);

        // Example: Send confirmation email, update stylist availability, etc.
    }

    @Override
    protected void onBookingConfirmed(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event) {
        // Handle booking confirmation
        log.info("Processing booking confirmation: {} for user: {} with stylist: {}", bookingId, userId, stylistId);

        // Example: Send calendar invites, update payment status, etc.
    }

    @Override
    protected void onBookingCancelled(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event) {
        // Handle booking cancellation
        log.info("Processing booking cancellation: {} for user: {} with stylist: {}", bookingId, userId, stylistId);

        // Example: Process refunds, send notifications, update availability, etc.
    }

    @Override
    protected void onBookingCompleted(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event) {
        // Handle booking completion
        log.info("Processing booking completion: {} for user: {} with stylist: {}", bookingId, userId, stylistId);

        // Example: Send review requests, update stylist ratings, process payments, etc.
    }

    @Override
    protected void onPaymentSucceeded(String paymentId, String userId, String amount, EventBridgeConsumerService.EventMessage event) {
        // Handle successful payment
        log.info("Processing successful payment: {} for user: {} amount: {}", paymentId, userId, amount);

        // Example: Update booking status, send confirmations, etc.
    }

    @Override
    protected void onPaymentFailed(String paymentId, String userId, String amount, EventBridgeConsumerService.EventMessage event) {
        // Handle failed payment
        log.info("Processing failed payment: {} for user: {} amount: {}", paymentId, userId, amount);

        // Example: Send failure notifications, cancel bookings, etc.
    }

    @Override
    protected void onPaymentRefunded(String paymentId, String userId, String amount, EventBridgeConsumerService.EventMessage event) {
        // Handle payment refund
        log.info("Processing payment refund: {} for user: {} amount: {}", paymentId, userId, amount);

        // Example: Update booking status, send refund confirmations, etc.
    }

    @Override
    protected void onImageJobCompleted(String jobId, String userId, EventBridgeConsumerService.EventMessage event) {
        // Handle completed image processing job
        log.info("Processing completed image job: {} for user: {}", jobId, userId);

        // Example: Send completion notifications, update user profile, etc.
    }

    @Override
    protected void onImageJobFailed(String jobId, String userId, EventBridgeConsumerService.EventMessage event) {
        // Handle failed image processing job
        log.info("Processing failed image job: {} for user: {}", jobId, userId);

        // Example: Send failure notifications, retry logic, etc.
    }
}
