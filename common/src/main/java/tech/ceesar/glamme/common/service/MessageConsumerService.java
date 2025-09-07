package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumerService {

    private final SqsConsumerService sqsConsumerService;

    /**
     * Generic message processor that can be configured for different queues
     */
    @Scheduled(fixedDelay = 10000) // Poll every 10 seconds
    public void processImageJobsQueue() {
        processQueue("image-jobs-queue-url", this::processImageJobMessage);
    }

    @Scheduled(fixedDelay = 15000) // Poll every 15 seconds
    public void processBookingEventsQueue() {
        processQueue("booking-events-queue-url", this::processBookingEventMessage);
    }

    @Scheduled(fixedDelay = 20000) // Poll every 20 seconds
    public void processPaymentEventsQueue() {
        processQueue("payment-events-queue-url", this::processPaymentEventMessage);
    }

    private void processQueue(String queueUrlProperty, Consumer<Message> messageProcessor) {
        try {
            String queueUrl = getQueueUrl(queueUrlProperty);
            if (queueUrl == null) {
                return;
            }

            List<Message> messages = sqsConsumerService.pollMessages(queueUrl, 5, 20);

            for (Message message : messages) {
                try {
                    messageProcessor.accept(message);
                    sqsConsumerService.deleteMessage(queueUrl, message.receiptHandle());
                } catch (Exception e) {
                    log.error("Error processing message: {}", message.messageId(), e);
                    // Extend visibility timeout to allow retry
                    sqsConsumerService.changeVisibilityTimeout(queueUrl, message.receiptHandle(), 120);
                }
            }

        } catch (Exception e) {
            log.error("Error processing queue: {}", queueUrlProperty, e);
        }
    }

    private void processImageJobMessage(Message message) {
        String jobId = message.body();
        log.info("Processing image job: {}", jobId);

        // This would typically delegate to an image processing service
        // For now, just log the processing
    }

    private void processBookingEventMessage(Message message) {
        String eventData = message.body();
        log.info("Processing booking event: {}", eventData);

        // This would typically parse the event and delegate to booking service
        // For now, just log the processing
    }

    private void processPaymentEventMessage(Message message) {
        String eventData = message.body();
        log.info("Processing payment event: {}", eventData);

        // This would typically parse the event and delegate to payment service
        // For now, just log the processing
    }

    private String getQueueUrl(String propertyName) {
        // This would typically get the queue URL from configuration
        // For now, return null to disable processing
        switch (propertyName) {
            case "image-jobs-queue-url":
                return System.getenv("IMAGE_JOBS_QUEUE_URL");
            case "booking-events-queue-url":
                return System.getenv("BOOKING_EVENTS_QUEUE_URL");
            case "payment-events-queue-url":
                return System.getenv("PAYMENT_EVENTS_QUEUE_URL");
            default:
                return null;
        }
    }
}
