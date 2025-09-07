package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqsConsumerService {

    private final SqsClient sqsClient;

    /**
     * Poll messages from SQS queue
     */
    public List<Message> pollMessages(String queueUrl, int maxMessages, int waitTimeSeconds) {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(waitTimeSeconds)
                    .visibilityTimeout(300) // 5 minutes
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            return response.messages();

        } catch (Exception e) {
            log.error("Error polling messages from queue: {}", queueUrl, e);
            return List.of();
        }
    }

    /**
     * Delete message from queue
     */
    public void deleteMessage(String queueUrl, String receiptHandle) {
        try {
            DeleteMessageRequest request = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();

            sqsClient.deleteMessage(request);
            log.debug("Deleted message from queue: {}", queueUrl);

        } catch (Exception e) {
            log.error("Error deleting message from queue: {}", queueUrl, e);
        }
    }

    /**
     * Change message visibility timeout
     */
    public void changeVisibilityTimeout(String queueUrl, String receiptHandle, int visibilityTimeoutSeconds) {
        try {
            ChangeMessageVisibilityRequest request = ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(visibilityTimeoutSeconds)
                    .build();

            sqsClient.changeMessageVisibility(request);
            log.debug("Changed visibility timeout for message in queue: {}", queueUrl);

        } catch (Exception e) {
            log.error("Error changing visibility timeout for message in queue: {}", queueUrl, e);
        }
    }

    /**
     * Send message to queue
     */
    public String sendMessage(String queueUrl, String messageBody, Map<String, String> messageAttributes) {
        try {
            SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody);

            if (messageAttributes != null && !messageAttributes.isEmpty()) {
                Map<String, MessageAttributeValue> attributes = new HashMap<>();
                messageAttributes.forEach((key, value) ->
                    attributes.put(key, MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(value)
                            .build()));
                requestBuilder.messageAttributes(attributes);
            }

            SendMessageRequest request = requestBuilder.build();
            SendMessageResponse response = sqsClient.sendMessage(request);

            log.debug("Sent message to queue: {}, messageId: {}", queueUrl, response.messageId());
            return response.messageId();

        } catch (Exception e) {
            log.error("Error sending message to queue: {}", queueUrl, e);
            return null;
        }
    }

    /**
     * Process messages asynchronously
     */
    @Async
    public CompletableFuture<Void> processMessagesAsync(String queueUrl,
                                                       Function<Message, Boolean> messageProcessor,
                                                       int maxMessages,
                                                       int waitTimeSeconds) {
        return CompletableFuture.runAsync(() -> {
            List<Message> messages = pollMessages(queueUrl, maxMessages, waitTimeSeconds);

            for (Message message : messages) {
                try {
                    boolean processed = messageProcessor.apply(message);
                    if (processed) {
                        deleteMessage(queueUrl, message.receiptHandle());
                    } else {
                        // Message processing failed, extend visibility timeout
                        changeVisibilityTimeout(queueUrl, message.receiptHandle(), 60);
                    }
                } catch (Exception e) {
                    log.error("Error processing message: {}", message.messageId(), e);
                    // Extend visibility timeout on error
                    changeVisibilityTimeout(queueUrl, message.receiptHandle(), 120);
                }
            }
        });
    }

    /**
     * Get queue attributes
     */
    public Map<QueueAttributeName, String> getQueueAttributes(String queueUrl) {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                                   QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)
                    .build();

            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
            return response.attributes();

        } catch (Exception e) {
            log.error("Error getting queue attributes for: {}", queueUrl, e);
            return Map.of();
        }
    }

    /**
     * Purge queue (use with caution)
     */
    public void purgeQueue(String queueUrl) {
        try {
            PurgeQueueRequest request = PurgeQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();

            sqsClient.purgeQueue(request);
            log.warn("Purged queue: {}", queueUrl);

        } catch (Exception e) {
            log.error("Error purging queue: {}", queueUrl, e);
        }
    }
}
