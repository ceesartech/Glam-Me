package tech.ceesar.glamme.communication.service.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import tech.ceesar.glamme.communication.dto.BulkSmsResponse;
import tech.ceesar.glamme.communication.dto.SmsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnsService {

    private final SnsClient snsClient;

    @Value("${aws.sns.topic-arn:}")
    private String defaultTopicArn;

    /**
     * Send SMS message
     */
    public SmsResponse sendSms(String phoneNumber, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(phoneNumber)
                    .message(message)
                    .messageAttributes(Map.of(
                            "AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("Transactional")
                                    .build()
                    ))
                    .build();

            PublishResponse response = snsClient.publish(request);

            log.info("Sent SMS to {}: messageId={}", phoneNumber, response.messageId());

            return SmsResponse.builder()
                    .messageId(response.messageId())
                    .timestamp(Instant.now())
                    .status("SENT")
                    .build();

        } catch (Exception e) {
            log.error("Failed to send SMS to {}", phoneNumber, e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    /**
     * Send SMS to multiple recipients
     */
    public BulkSmsResponse sendBulkSms(List<String> phoneNumbers, String message) {
        List<BulkSmsResponse.Success> successes = new ArrayList<>();
        List<BulkSmsResponse.Failure> failures = new ArrayList<>();

        for (String phoneNumber : phoneNumbers) {
            try {
                SmsResponse smsResponse = sendSms(phoneNumber, message);
                successes.add(BulkSmsResponse.Success.builder()
                        .messageId(smsResponse.getMessageId())
                        .build());
            } catch (Exception e) {
                failures.add(BulkSmsResponse.Failure.builder()
                        .error(e.getMessage())
                        .build());
            }
        }

        return BulkSmsResponse.builder()
                .success(successes)
                .failure(failures)
                .build();
    }

    /**
     * Subscribe phone number to topic
     */
    public String subscribePhoneNumber(String phoneNumber) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .topicArn(defaultTopicArn)
                    .protocol("sms")
                    .endpoint(phoneNumber)
                    .build();

            SubscribeResponse response = snsClient.subscribe(request);

            log.info("Subscribed {} to topic {}", phoneNumber, defaultTopicArn);

            return response.subscriptionArn();

        } catch (Exception e) {
            log.error("Failed to subscribe {} to topic", phoneNumber, e);
            throw new RuntimeException("Failed to subscribe phone number", e);
        }
    }

    /**
     * Unsubscribe phone number from topic
     */
    public void unsubscribePhoneNumber(String subscriptionArn) {
        try {
            UnsubscribeRequest request = UnsubscribeRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .build();

            snsClient.unsubscribe(request);

            log.info("Unsubscribed from topic: {}", subscriptionArn);

        } catch (Exception e) {
            log.error("Failed to unsubscribe from topic: {}", subscriptionArn, e);
        }
    }

    /**
     * Create SMS topic
     */
    public String createSmsTopic(String topicName) {
        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .attributes(Map.of(
                            "DisplayName", topicName,
                            "DeliveryPolicy", "{\"healthyRetryPolicy\":{\"numRetries\":3}}"
                    ))
                    .build();

            CreateTopicResponse response = snsClient.createTopic(request);

            log.info("Created SMS topic: {}", response.topicArn());

            return response.topicArn();

        } catch (Exception e) {
            log.error("Failed to create SMS topic: {}", topicName, e);
            throw new RuntimeException("Failed to create SMS topic", e);
        }
    }

    /**
     * Delete SMS topic
     */
    public void deleteSmsTopic(String topicArn) {
        try {
            DeleteTopicRequest request = DeleteTopicRequest.builder()
                    .topicArn(topicArn)
                    .build();

            snsClient.deleteTopic(request);

            log.info("Deleted SMS topic: {}", topicArn);

        } catch (Exception e) {
            log.error("Failed to delete SMS topic: {}", topicArn, e);
        }
    }

    /**
     * Get SMS delivery status (if enabled)
     */
    public String getSmsDeliveryStatus(String messageId) {
        // Note: SMS delivery status requires additional AWS configuration
        // This is a placeholder for future implementation
        log.debug("SMS delivery status check for message: {}", messageId);
        return "DELIVERED"; // Placeholder
    }

    /**
     * Send promotional SMS (different pricing)
     */
    public SmsResponse sendPromotionalSms(String phoneNumber, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(phoneNumber)
                    .message(message)
                    .messageAttributes(Map.of(
                            "AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("Promotional")
                                    .build()
                    ))
                    .build();

            PublishResponse response = snsClient.publish(request);

            log.info("Sent promotional SMS to {}: messageId={}", phoneNumber, response.messageId());

            return SmsResponse.builder()
                    .messageId(response.messageId())
                    .timestamp(Instant.now())
                    .status("SENT")
                    .build();

        } catch (Exception e) {
            log.error("Failed to send promotional SMS to {}", phoneNumber, e);
            throw new RuntimeException("Failed to send promotional SMS", e);
        }
    }

}
