package tech.ceesar.glamme.communication.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.communication.dto.*;
import tech.ceesar.glamme.communication.service.CommunicationService;
import tech.ceesar.glamme.communication.service.aws.PinpointService;

/**
 * Simple Communication Controller for AWS deployment
 * Provides basic messaging functionality
 */
@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
@Slf4j
public class CommunicationController {

    private final CommunicationService communicationService;
    private final PinpointService pinpointService;

    /**
     * Send SMS via AWS Pinpoint
     */
    @PostMapping("/sms")
    public ResponseEntity<SendSmsResponse> sendSms(@RequestBody SendSmsRequest req) {
        log.info("Received SMS request to: {}", req.getToNumber());
        SendSmsResponse response = communicationService.sendSms(req);
        return ResponseEntity.ok(response);
    }

    /**
     * Send push notification via AWS Pinpoint
     */
    @PostMapping("/push")
    public ResponseEntity<PushResponse> sendPushNotification(@RequestBody PushRequest req) {
        PushResponse response = communicationService.sendPushNotification(req);
        return ResponseEntity.ok(response);
    }

    /**
     * Send email (simplified implementation)
     */
    @PostMapping("/email")
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody EmailRequest req) {
        EmailResponse response = communicationService.sendEmail(req);
        return ResponseEntity.ok(response);
    }

    /**
     * Create video meeting (simplified implementation)
     */
    @PostMapping("/video/meeting")
    public ResponseEntity<CallResponse> createVideoMeeting(@RequestBody CallRequest req) {
        CallResponse response = communicationService.initiateCall(req);
        return ResponseEntity.ok(response);
    }

    /**
     * Send bulk SMS via AWS Pinpoint
     */
    @PostMapping("/sms/bulk")
    public ResponseEntity<BulkSmsResponse> sendBulkSms(@RequestBody BulkSmsRequest req) {
        BulkSmsResponse response = pinpointService.sendBulkSms(req.getPhoneNumbers(), req.getMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Send bulk email
     */
    @PostMapping("/email/bulk")
    public ResponseEntity<BulkEmailResponse> sendBulkEmail(@RequestBody BulkEmailRequest req) {
        BulkEmailResponse response = communicationService.sendBulkEmail(req);
        return ResponseEntity.ok(response);
    }

    /**
     * Get SMS statistics
     */
    @GetMapping("/stats/sms")
    public ResponseEntity<SmsStats> getSmsStats() {
        SmsStats stats = communicationService.getSmsStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get email statistics
     */
    @GetMapping("/stats/email")
    public ResponseEntity<EmailStats> getEmailStats() {
        EmailStats stats = communicationService.getEmailStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Create video meeting
     */
    @PostMapping("/meeting")
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody MeetingDetails details) {
        MeetingResponse response = communicationService.createMeeting(details);
        return ResponseEntity.ok(response);
    }

    /**
     * Join video meeting
     */
    @PostMapping("/meeting/{meetingId}/join")
    public ResponseEntity<MeetingResponse> joinMeeting(@PathVariable String meetingId, @RequestBody AttendeeInfo attendee) {
        MeetingResponse response = communicationService.joinMeeting(meetingId, attendee);
        return ResponseEntity.ok(response);
    }

    /**
     * Get attendee information
     */
    @GetMapping("/meeting/{meetingId}/attendee/{attendeeId}")
    public ResponseEntity<AttendeeResponse> getAttendeeInfo(@PathVariable String meetingId, @PathVariable String attendeeId) {
        AttendeeResponse response = communicationService.getAttendeeInfo(meetingId, attendeeId);
        return ResponseEntity.ok(response);
    }

    /**
     * End call/meeting
     */
    @PostMapping("/call/{callId}/end")
    public ResponseEntity<CallResponse> endCall(@PathVariable String callId) {
        CallResponse response = communicationService.endCall(callId);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(communicationService.getHealthStatus());
    }
}

