package tech.ceesar.glamme.communication.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.communication.dto.SendSmsRequest;
import tech.ceesar.glamme.communication.dto.SendSmsResponse;
import tech.ceesar.glamme.communication.service.SimpleCommunicationService;

/**
 * Simple Communication Controller for AWS deployment
 */
@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
@Slf4j
public class SimpleCommunicationController {

    private final SimpleCommunicationService communicationService;

    /**
     * Send SMS via AWS services
     */
    @PostMapping("/sms")
    public ResponseEntity<SendSmsResponse> sendSms(@RequestBody SendSmsRequest request) {
        log.info("Received SMS request to: {}", request.getToNumber());
        SendSmsResponse response = communicationService.sendSms(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        String status = communicationService.getHealthStatus();
        return ResponseEntity.ok(status);
    }
}
