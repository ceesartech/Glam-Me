package tech.ceesar.glamme.communication.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.communication.dto.CallRequest;
import tech.ceesar.glamme.communication.dto.CallResponse;
import tech.ceesar.glamme.communication.dto.SendSmsRequest;
import tech.ceesar.glamme.communication.dto.SendSmsResponse;
import tech.ceesar.glamme.communication.service.CommunicationService;

@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
public class CommunicationController {
    private final CommunicationService communicationService;

    /**
     * Send SMS.
     */
    @PostMapping("/sms")
    public ResponseEntity<SendSmsResponse> sendSms(
            @RequestBody SendSmsRequest req) {
        return ResponseEntity.ok(communicationService.sendSms(req));
    }

    /**
     * Twilio status callback for SMS.
     */
    @PostMapping("/sms/status")
    public ResponseEntity<Void> smsStatus(
            @RequestParam("MessageSid") String sid,
            @RequestParam("MessageStatus") String status) {
        communicationService.handleSmsStatusCallback(sid, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Twilio webhook for inbound SMS.
     */
    @PostMapping("/sms/incoming")
    public ResponseEntity<Void> inboundSms(
            @RequestParam("From") String from,
            @RequestParam("To") String to,
            @RequestParam("Body") String body,
            @RequestParam("MessageSid") String sid) {
        communicationService.handleIncomingSms(from, to, body, sid);
        return ResponseEntity.ok().build();
    }

    /**
     * Initiate a voice call.
     */
    @PostMapping("/voice/call")
    public ResponseEntity<CallResponse> call(
            @RequestBody CallRequest req) {
        return ResponseEntity.ok(communicationService.initiateCall(req));
    }

    /**
     * Status callback for voice calls.
     */
    @PostMapping("/voice/status")
    public ResponseEntity<Void> callStatus(
            @RequestParam("CallSid") String sid,
            @RequestParam("CallStatus") String status) {
        communicationService.handleCallStatusCallback(sid, status);
        return ResponseEntity.ok().build();
    }

    /**
     * TwiML endpoint for outbound calls: bridges to ?to=targetNumber
     */
    @GetMapping(value = "/voice/twiml", produces = MediaType.APPLICATION_XML_VALUE)
    public String twiml(@RequestParam("to") String to) {
        return "<Response><Dial>" + to + "</Dial></Response>";
    }
}
