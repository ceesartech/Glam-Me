package tech.ceesar.glamme.communication.service.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import tech.ceesar.glamme.communication.dto.EmailResponse;
import tech.ceesar.glamme.communication.dto.EmailStats;
import tech.ceesar.glamme.communication.dto.BulkEmailResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SesService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email:noreply@glamme.com}")
    private String fromEmail;

    /**
     * Send simple text email
     */
    public EmailResponse sendEmail(String toAddress, String subject, String body) {
        return sendEmail(toAddress, subject, body, null);
    }

    /**
     * Send email with optional HTML body
     */
    public EmailResponse sendEmail(String toAddress, String subject, String body, String htmlBody) {
        if (htmlBody != null && !htmlBody.trim().isEmpty()) {
            return sendHtmlEmail(toAddress, subject, htmlBody, body);
        } else {
            return sendTextEmail(toAddress, subject, body);
        }
    }

    /**
     * Send text email
     */
    private EmailResponse sendTextEmail(String toAddress, String subject, String body) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toAddress)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data(body)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);

            log.info("Sent text email to {}: messageId={}", toAddress, response.messageId());

            return EmailResponse.builder()
                    .messageId(response.messageId())
                    .timestamp(Instant.now())
                    .successfulRecipients(List.of(toAddress))
                    .build();

        } catch (Exception e) {
            log.error("Failed to send text email to {}", toAddress, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send HTML email
     */
    public EmailResponse sendHtmlEmail(String toAddress, String subject, String htmlBody, String textBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toAddress)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .data(htmlBody)
                                            .charset("UTF-8")
                                            .build())
                                    .text(Content.builder()
                                            .data(textBody)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);

            log.info("Sent HTML email to {}: messageId={}", toAddress, response.messageId());

            return EmailResponse.builder()
                    .messageId(response.messageId())
                    .timestamp(Instant.now())
                    .successfulRecipients(List.of(toAddress))
                    .build();

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", toAddress, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Send email to multiple recipients
     */
    public BulkEmailResponse sendBulkEmail(List<String> toAddresses, String subject, String body) {
        List<BulkEmailResponse.Success> successes = new java.util.ArrayList<>();
        List<BulkEmailResponse.Failure> failures = new java.util.ArrayList<>();

        for (String toAddress : toAddresses) {
            try {
                EmailResponse emailResponse = sendEmail(toAddress, subject, body);
                successes.add(BulkEmailResponse.Success.builder()
                        .messageId(emailResponse.getMessageId())
                        .email(toAddress)
                        .build());
            } catch (Exception e) {
                failures.add(BulkEmailResponse.Failure.builder()
                        .email(toAddress)
                        .error(e.getMessage())
                        .errorCode("SEND_FAILED")
                        .build());
            }
        }

        return BulkEmailResponse.builder()
                .success(successes)
                .failure(failures)
                .build();
    }

    /**
     * Send templated email
     */
    public EmailResponse sendTemplatedEmail(String toAddress, String templateName,
                                          Map<String, String> templateData) {
        try {
            String templateJson = objectMapper.writeValueAsString(templateData);

            SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toAddress)
                            .build())
                    .template(templateName)
                    .templateData(templateJson)
                    .build();

            SendTemplatedEmailResponse response = sesClient.sendTemplatedEmail(request);

            log.info("Sent templated email to {} using template {}: messageId={}",
                    toAddress, templateName, response.messageId());

            return EmailResponse.builder()
                    .messageId(response.messageId())
                    .timestamp(Instant.now())
                    .successfulRecipients(List.of(toAddress))
                    .build();

        } catch (Exception e) {
            log.error("Failed to send templated email to {}", toAddress, e);
            throw new RuntimeException("Failed to send templated email", e);
        }
    }

    /**
     * Verify email address for sending
     */
    public void verifyEmailAddress(String emailAddress) {
        try {
            VerifyEmailIdentityRequest request = VerifyEmailIdentityRequest.builder()
                    .emailAddress(emailAddress)
                    .build();

            sesClient.verifyEmailIdentity(request);

            log.info("Verification email sent to: {}", emailAddress);

        } catch (Exception e) {
            log.error("Failed to verify email address: {}", emailAddress, e);
            throw new RuntimeException("Failed to verify email address", e);
        }
    }

    /**
     * Check if email address is verified
     */
    public boolean isEmailVerified(String emailAddress) {
        try {
            GetIdentityVerificationAttributesRequest request = GetIdentityVerificationAttributesRequest.builder()
                    .identities(emailAddress)
                    .build();

            GetIdentityVerificationAttributesResponse response = sesClient.getIdentityVerificationAttributes(request);

            var attributes = response.verificationAttributes().get(emailAddress);
            return attributes != null && "Success".equals(attributes.verificationStatus().toString());

        } catch (Exception e) {
            log.error("Failed to check email verification status: {}", emailAddress, e);
            return false;
        }
    }

    /**
     * Create email template
     */
    public void createEmailTemplate(String templateName, String subject, String htmlBody, String textBody) {
        try {
            CreateTemplateRequest request = CreateTemplateRequest.builder()
                    .template(Template.builder()
                            .templateName(templateName)
                            .subjectPart(subject)
                            .htmlPart(htmlBody)
                            .textPart(textBody)
                            .build())
                    .build();

            sesClient.createTemplate(request);

            log.info("Created email template: {}", templateName);

        } catch (Exception e) {
            log.error("Failed to create email template: {}", templateName, e);
            throw new RuntimeException("Failed to create email template", e);
        }
    }

    /**
     * Update email template
     */
    public void updateEmailTemplate(String templateName, String subject, String htmlBody, String textBody) {
        try {
            UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                    .template(Template.builder()
                            .templateName(templateName)
                            .subjectPart(subject)
                            .htmlPart(htmlBody)
                            .textPart(textBody)
                            .build())
                    .build();

            sesClient.updateTemplate(request);

            log.info("Updated email template: {}", templateName);

        } catch (Exception e) {
            log.error("Failed to update email template: {}", templateName, e);
            throw new RuntimeException("Failed to update email template", e);
        }
    }

    /**
     * Delete email template
     */
    public void deleteEmailTemplate(String templateName) {
        try {
            DeleteTemplateRequest request = DeleteTemplateRequest.builder()
                    .templateName(templateName)
                    .build();

            sesClient.deleteTemplate(request);

            log.info("Deleted email template: {}", templateName);

        } catch (Exception e) {
            log.error("Failed to delete email template: {}", templateName, e);
            throw new RuntimeException("Failed to delete email template", e);
        }
    }

    /**
     * Get email sending statistics
     */
    public EmailStats getEmailStats() {
        try {
            GetSendStatisticsRequest request = GetSendStatisticsRequest.builder().build();
            GetSendStatisticsResponse response = sesClient.getSendStatistics(request);

            // Aggregate statistics from the last 24 hours
            long totalSent = response.sendDataPoints().stream()
                    .mapToLong(SendDataPoint::deliveryAttempts)
                    .sum();

            long bounces = response.sendDataPoints().stream()
                    .mapToLong(SendDataPoint::bounces)
                    .sum();

            long complaints = response.sendDataPoints().stream()
                    .mapToLong(SendDataPoint::complaints)
                    .sum();

            long rejects = response.sendDataPoints().stream()
                    .mapToLong(SendDataPoint::rejects)
                    .sum();

            return EmailStats.builder()
                    .totalSent(totalSent)
                    .totalBounced(bounces)
                    .totalComplained(complaints)
                    .deliveryRate(totalSent > 0 ? ((double) (totalSent - bounces - rejects) / totalSent) * 100 : 0)
                    .bounceRate(totalSent > 0 ? ((double) bounces / totalSent) * 100 : 0)
                    .complaintRate(totalSent > 0 ? ((double) complaints / totalSent) * 100 : 0)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get email statistics", e);
            return EmailStats.builder().build();
        }
    }

    // Helper method for JSON serialization
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
}
