package tech.ceesar.glamme.communication.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.communication.dto.CallRequest;
import tech.ceesar.glamme.communication.dto.CallResponse;
import tech.ceesar.glamme.communication.dto.SendSmsRequest;
import tech.ceesar.glamme.communication.dto.SendSmsResponse;
import tech.ceesar.glamme.communication.dto.PushResponse;
import tech.ceesar.glamme.communication.dto.EmailResponse;
import tech.ceesar.glamme.communication.dto.BulkSmsResponse;
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
        PushResponse response = communicationService.sendPushNotification(
                req.deviceToken(), req.title(), req.body());
        return ResponseEntity.ok(response);
    }

    /**
     * Send email (simplified implementation)
     */
    @PostMapping("/email")
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody EmailRequest req) {
        EmailResponse response = communicationService.sendEmail(req.toAddress(), req.subject(), req.body());
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
        BulkSmsResponse response = pinpointService.sendBulkSms(req.phoneNumbers(), req.message());
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Communication service is operational with AWS integration");
    }
}

// Simple request DTOs for the controller
record PushRequest(String deviceToken, String title, String body) {}
record EmailRequest(String toAddress, String subject, String body) {}
record BulkSmsRequest(java.util.List<String> phoneNumbers, String message) {}
