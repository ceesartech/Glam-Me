package tech.ceesar.glamme.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.ceesar.glamme.common.service.EventBridgeConsumerService;

@Component
@RequiredArgsConstructor
@Slf4j
public abstract class BaseEventHandler {

    protected final EventBridgeConsumerService eventBridgeConsumer;

    /**
     * Register all event handlers
     */
    public abstract void registerHandlers();

    /**
     * Handle user-related events
     */
    protected void handleUserEvent(EventBridgeConsumerService.EventMessage event) {
        String eventType = event.getEventType();
        String userId = event.getDetailAsString("userId");

        log.info("Handling user event: {} for user: {}", eventType, userId);

        switch (eventType) {
            case "user.registered":
                onUserRegistered(userId, event);
                break;
            case "user.profile.updated":
                onUserProfileUpdated(userId, event);
                break;
            case "user.deleted":
                onUserDeleted(userId, event);
                break;
            default:
                log.warn("Unknown user event type: {}", eventType);
        }
    }

    /**
     * Handle booking-related events
     */
    protected void handleBookingEvent(EventBridgeConsumerService.EventMessage event) {
        String eventType = event.getEventType();
        String bookingId = event.getDetailAsString("bookingId");
        String userId = event.getDetailAsString("userId");
        String stylistId = event.getDetailAsString("stylistId");

        log.info("Handling booking event: {} for booking: {}", eventType, bookingId);

        switch (eventType) {
            case "booking.created":
                onBookingCreated(bookingId, userId, stylistId, event);
                break;
            case "booking.confirmed":
                onBookingConfirmed(bookingId, userId, stylistId, event);
                break;
            case "booking.cancelled":
                onBookingCancelled(bookingId, userId, stylistId, event);
                break;
            case "booking.completed":
                onBookingCompleted(bookingId, userId, stylistId, event);
                break;
            default:
                log.warn("Unknown booking event type: {}", eventType);
        }
    }

    /**
     * Handle payment-related events
     */
    protected void handlePaymentEvent(EventBridgeConsumerService.EventMessage event) {
        String eventType = event.getEventType();
        String paymentId = event.getDetailAsString("paymentId");
        String userId = event.getDetailAsString("userId");
        String amount = event.getDetailAsString("amount");

        log.info("Handling payment event: {} for payment: {}", eventType, paymentId);

        switch (eventType) {
            case "payment.succeeded":
                onPaymentSucceeded(paymentId, userId, amount, event);
                break;
            case "payment.failed":
                onPaymentFailed(paymentId, userId, amount, event);
                break;
            case "payment.refunded":
                onPaymentRefunded(paymentId, userId, amount, event);
                break;
            default:
                log.warn("Unknown payment event type: {}", eventType);
        }
    }

    /**
     * Handle image processing events
     */
    protected void handleImageEvent(EventBridgeConsumerService.EventMessage event) {
        String eventType = event.getEventType();
        String jobId = event.getDetailAsString("jobId");
        String userId = event.getDetailAsString("userId");

        log.info("Handling image event: {} for job: {}", eventType, jobId);

        switch (eventType) {
            case "image.job.completed":
                onImageJobCompleted(jobId, userId, event);
                break;
            case "image.job.failed":
                onImageJobFailed(jobId, userId, event);
                break;
            default:
                log.warn("Unknown image event type: {}", eventType);
        }
    }

    // Abstract methods to be implemented by concrete handlers

    protected abstract void onUserRegistered(String userId, EventBridgeConsumerService.EventMessage event);
    protected abstract void onUserProfileUpdated(String userId, EventBridgeConsumerService.EventMessage event);
    protected abstract void onUserDeleted(String userId, EventBridgeConsumerService.EventMessage event);

    protected abstract void onBookingCreated(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event);
    protected abstract void onBookingConfirmed(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event);
    protected abstract void onBookingCancelled(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event);
    protected abstract void onBookingCompleted(String bookingId, String userId, String stylistId, EventBridgeConsumerService.EventMessage event);

    protected abstract void onPaymentSucceeded(String paymentId, String userId, String amount, EventBridgeConsumerService.EventMessage event);
    protected abstract void onPaymentFailed(String paymentId, String userId, String amount, EventBridgeConsumerService.EventMessage event);
    protected abstract void onPaymentRefunded(String paymentId, String userId, String amount, EventBridgeConsumerService.EventMessage event);

    protected abstract void onImageJobCompleted(String jobId, String userId, EventBridgeConsumerService.EventMessage event);
    protected abstract void onImageJobFailed(String jobId, String userId, EventBridgeConsumerService.EventMessage event);
}
