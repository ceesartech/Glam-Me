package tech.ceesar.glamme.communication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.communication.dto.CallRequest;
import tech.ceesar.glamme.communication.dto.CallResponse;
import tech.ceesar.glamme.communication.dto.SendSmsRequest;
import tech.ceesar.glamme.communication.dto.SendSmsResponse;
import tech.ceesar.glamme.communication.dto.PushResponse;
import tech.ceesar.glamme.communication.dto.EmailResponse;
import tech.ceesar.glamme.communication.entity.CommunicationLog;
import tech.ceesar.glamme.communication.enums.Channel;
import tech.ceesar.glamme.communication.enums.Direction;
import tech.ceesar.glamme.communication.repository.CommunicationLogRepository;
import tech.ceesar.glamme.communication.service.aws.PinpointService;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.common.service.RedisCacheService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationService {

    private final PinpointService pinpointService;
    private final CommunicationLogRepository logRepository;
    private final EventPublisher eventPublisher;
    private final RedisCacheService cacheService;

    /**
     * Send SMS using AWS Pinpoint
     */
    @Transactional
    public SendSmsResponse sendSms(SendSmsRequest smsRequest) {
        try {
            var smsResponse = pinpointService.sendSms(smsRequest.getToNumber(), smsRequest.getMessage());

            // Record log
            CommunicationLog commLog = CommunicationLog.builder()
                    .fromNumber(smsRequest.getFromNumber())
                    .toNumber(smsRequest.getToNumber())
                    .channel(Channel.SMS)
                    .direction(Direction.OUTBOUND)
                    .messageBody(smsRequest.getMessage())
                    .sid(smsResponse.getMessageId())
                    .status(smsResponse.getStatus())
                    .build();
            logRepository.save(commLog);

            // Cache the SMS response briefly
            cacheService.set("sms:" + smsResponse.getMessageId(), smsResponse, Duration.ofHours(24));

            // Publish SMS sent event
            eventPublisher.publishEvent("sms.sent", Map.of(
                    "messageId", smsResponse.getMessageId(),
                    "toNumber", smsRequest.getToNumber(),
                    "fromNumber", smsRequest.getFromNumber()
            ));

            log.info("SMS sent successfully to {}: messageId={}", smsRequest.getToNumber(), smsResponse.getMessageId());

            return new SendSmsResponse(smsResponse.getMessageId(), smsResponse.getStatus());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}", smsRequest.getToNumber(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    /**
     * Send push notification using Pinpoint
     */
    @Transactional
    public PushResponse sendPushNotification(String deviceToken, String title, String body) {
        try {
            var pushResponse = pinpointService.sendPushNotification(deviceToken, title, body);

            // Record log
            CommunicationLog commLog = CommunicationLog.builder()
                    .toNumber(deviceToken)
                    .channel(Channel.PUSH)
                    .direction(Direction.OUTBOUND)
                    .messageBody(body)
                    .sid(pushResponse.getMessageId())
                    .status(pushResponse.getStatus())
                    .build();
            logRepository.save(commLog);

            log.info("Push notification sent to device {}: messageId={}", deviceToken, pushResponse.getMessageId());

            return pushResponse;

        } catch (Exception e) {
            log.error("Failed to send push notification to device {}", deviceToken, e);
            throw new RuntimeException("Failed to send push notification", e);
        }
    }

    /**
     * Simple email sending (placeholder for future SES integration)
     */
    @Transactional
    public EmailResponse sendEmail(String toAddress, String subject, String body) {
        try {
            // Placeholder implementation - would use SES in production
            String messageId = "email-" + System.currentTimeMillis();

            // Record log
            CommunicationLog commLog = CommunicationLog.builder()
                    .toNumber(toAddress)
                    .channel(Channel.EMAIL)
                    .direction(Direction.OUTBOUND)
                    .messageBody(body)
                    .sid(messageId)
                    .status("SENT")
                    .build();
            logRepository.save(commLog);

            log.info("Email sent successfully to {}: messageId={}", toAddress, messageId);

            return EmailResponse.builder()
                    .messageId(messageId)
                    .timestamp(java.time.Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to send email to {}", toAddress, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Simple video meeting creation (placeholder for future Chime integration)
     */
    @Transactional
    public CallResponse initiateCall(CallRequest callRequest) {
        try {
            // Placeholder implementation - would use Chime in production
            String meetingId = "meeting-" + System.currentTimeMillis();

            // Record log
            CommunicationLog commLog = CommunicationLog.builder()
                    .fromNumber(callRequest.getFromNumber())
                    .toNumber(callRequest.getToNumber())
                    .channel(Channel.VIDEO)
                    .direction(Direction.OUTBOUND)
                    .sid(meetingId)
                    .status("CREATED")
                    .build();
            logRepository.save(commLog);

            log.info("Video meeting created: {} between stylist {} and client {}",
                    meetingId, callRequest.getFromNumber(), callRequest.getToNumber());

            return CallResponse.builder()
                    .callSid(meetingId)
                    .status("CREATED")
                    .build();

        } catch (Exception e) {
            log.error("Failed to create video meeting between {} and {}", callRequest.getFromNumber(), callRequest.getToNumber(), e);
            throw new RuntimeException("Failed to create meeting", e);
        }
    }

    /**
     * Update call/meeting status
     */
    @Transactional
    public void updateCallStatus(String meetingId, String status) {
        try {
            Optional<CommunicationLog> opt = logRepository.findBySid(meetingId);
            if (opt.isPresent()) {
                CommunicationLog commLog = opt.get();
                commLog.setStatus(status);
                logRepository.save(commLog);

                log.info("Call status updated: meetingId={}, status={}", meetingId, status);
            }
        } catch (Exception e) {
            log.error("Failed to update call status: {}", meetingId, e);
        }
    }
}
