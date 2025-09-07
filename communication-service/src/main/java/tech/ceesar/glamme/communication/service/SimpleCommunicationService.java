package tech.ceesar.glamme.communication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.communication.dto.*;
import tech.ceesar.glamme.communication.entity.CommunicationLog;
import tech.ceesar.glamme.communication.enums.Channel;
import tech.ceesar.glamme.communication.enums.Direction;
import tech.ceesar.glamme.communication.repository.CommunicationLogRepository;
import tech.ceesar.glamme.communication.service.aws.PinpointService;
import tech.ceesar.glamme.communication.service.aws.SesService;
import tech.ceesar.glamme.communication.service.aws.ChimeService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete Communication Service Implementation
 * Provides comprehensive communication functionality using AWS services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleCommunicationService implements CommunicationService {

    private final CommunicationLogRepository logRepository;
    private final PinpointService pinpointService;
    private final SesService sesService;
    private final ChimeService chimeService;

    // ==================== SMS Functionality ====================

    @Override
    @Transactional
    public SendSmsResponse sendSms(SendSmsRequest smsRequest) {
        try {
            // Use Pinpoint service for SMS
            SmsResponse smsResponse = pinpointService.sendSms(smsRequest.getToNumber(), smsRequest.getMessage());

            // Record log
            CommunicationLog logEntry = CommunicationLog.builder()
                    .fromNumber(smsRequest.getFromNumber())
                    .toNumber(smsRequest.getToNumber())
                    .channel(Channel.SMS)
                    .direction(Direction.OUTBOUND)
                    .messageBody(smsRequest.getMessage())
                    .sid(smsResponse.getMessageId())
                    .status("SENT")
                    .build();
            logRepository.save(logEntry);

            log.info("SMS sent successfully to {}: messageId={}", smsRequest.getToNumber(), smsResponse.getMessageId());

            return new SendSmsResponse(smsResponse.getMessageId(), "SENT");

        } catch (Exception e) {
            log.error("Failed to send SMS to {}", smsRequest.getToNumber(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    @Override
    @Transactional
    public BulkSmsResponse sendBulkSms(BulkSmsRequest request) {
        return pinpointService.sendBulkSms(request.getPhoneNumbers(), request.getMessage());
    }

    // ==================== Push Notifications ====================

    @Override
    public PushResponse sendPushNotification(String deviceToken, String title, String body) {
        return pinpointService.sendPushNotification(deviceToken, title, body);
    }

    @Override
    public PushResponse sendPushNotification(PushRequest request) {
        return pinpointService.sendPushNotification(request.getDeviceToken(), request.getTitle(), request.getBody());
    }

    // ==================== Email Functionality ====================

    @Override
    public EmailResponse sendEmail(String toAddress, String subject, String body) {
        return sendEmail(toAddress, subject, body, null);
    }

    @Override
    public EmailResponse sendEmail(String toAddress, String subject, String body, String htmlBody) {
        return sesService.sendEmail(toAddress, subject, body, htmlBody);
    }

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        return sendEmail(request.getToAddress(), request.getSubject(), request.getBody(), request.getHtmlBody());
    }

    @Override
    public BulkEmailResponse sendBulkEmail(BulkEmailRequest request) {
        List<BulkEmailResponse.Success> successes = new ArrayList<>();
        List<BulkEmailResponse.Failure> failures = new ArrayList<>();

        for (String email : request.getToAddresses()) {
            try {
                EmailResponse response = sendEmail(email, request.getSubject(), request.getBody(), request.getHtmlBody());
                successes.add(BulkEmailResponse.Success.builder()
                        .messageId(response.getMessageId())
                        .email(email)
                        .build());
            } catch (Exception e) {
                failures.add(BulkEmailResponse.Failure.builder()
                        .email(email)
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

    // ==================== Video Calling & Meetings ====================

    @Override
    public CallResponse initiateCall(CallRequest request) {
        return chimeService.initiateCall(request);
    }

    @Override
    public MeetingResponse createMeeting(MeetingDetails details) {
        return chimeService.createMeeting(details);
    }

    @Override
    public MeetingResponse joinMeeting(String meetingId, AttendeeInfo attendee) {
        return chimeService.joinMeeting(meetingId, attendee);
    }

    @Override
    public AttendeeResponse getAttendeeInfo(String meetingId, String attendeeId) {
        return chimeService.getAttendeeInfo(meetingId, attendeeId);
    }

    @Override
    public CallResponse endCall(String callId) {
        return chimeService.endCall(callId);
    }

    // ==================== Statistics ====================

    @Override
    public SmsStats getSmsStats() {
        return pinpointService.getSmsStats();
    }

    @Override
    public EmailStats getEmailStats() {
        return sesService.getEmailStats();
    }

    // ==================== Health Check ====================

    @Override
    public String getHealthStatus() {
        return "Communication service is operational with complete AWS integration";
    }
}
