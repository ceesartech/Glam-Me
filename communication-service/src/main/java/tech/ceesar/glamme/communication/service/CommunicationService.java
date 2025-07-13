package tech.ceesar.glamme.communication.service;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.communication.client.TwilioClient;
import tech.ceesar.glamme.communication.dto.CallRequest;
import tech.ceesar.glamme.communication.dto.CallResponse;
import tech.ceesar.glamme.communication.dto.SendSmsRequest;
import tech.ceesar.glamme.communication.dto.SendSmsResponse;
import tech.ceesar.glamme.communication.entity.CommunicationLog;
import tech.ceesar.glamme.communication.enums.Channel;
import tech.ceesar.glamme.communication.enums.Direction;
import tech.ceesar.glamme.communication.repository.CommunicationLogRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunicationService {
    private final TwilioClient twilioClient;
    private final CommunicationLogRepository logRepository;

    @Transactional
    public SendSmsResponse sendSms(SendSmsRequest smsRequest) {
        // 1) send via Twilio
        Message message = twilioClient.sendSms(smsRequest.getToNumber(), smsRequest.getMessage());
        // 2) record log
        CommunicationLog log = CommunicationLog.builder()
                .fromNumber(smsRequest.getFromNumber())
                .toNumber(smsRequest.getToNumber())
                .channel(Channel.SMS)
                .direction(Direction.OUTBOUND)
                .messageBody(smsRequest.getMessage())
                .sid(message.getSid())
                .status(message.getStatus().toString())
                .build();
        logRepository.save(log);
        return new SendSmsResponse(message.getSid(), message.getStatus().toString());
    }

    @Transactional
    public CallResponse initiateCall(CallRequest callRequest) {
        Call call = twilioClient.createCall(callRequest.getToNumber());
        CommunicationLog log = CommunicationLog.builder()
                .fromNumber(callRequest.getFromNumber())
                .toNumber(callRequest.getToNumber())
                .channel(Channel.VOICE)
                .direction(Direction.OUTBOUND)
                .sid(call.getSid())
                .status(call.getStatus().toString())
                .build();
        logRepository.save(log);
        return new CallResponse(call.getSid(), call.getStatus().toString());
    }

    @Transactional
    public void handleSmsStatusCallback(String sid, String status) {
        Optional<CommunicationLog> opt = logRepository.findBySid(sid);
        opt.ifPresent(log -> {
            log.setStatus(status);
            logRepository.save(log);
        });
    }

    @Transactional
    public void handleIncomingSms(String from, String to, String body, String sid) {
        CommunicationLog log = CommunicationLog.builder()
                .fromNumber(from)
                .toNumber(to)
                .channel(Channel.SMS)
                .direction(Direction.INBOUND)
                .messageBody(body)
                .sid(sid)
                .status("received")
                .build();
        logRepository.save(log);
    }

    @Transactional
    public void handleCallStatusCallback(String sid, String status) {
        Optional<CommunicationLog> opt = logRepository.findBySid(sid);
        opt.ifPresent(log -> {
            log.setStatus(status);
            logRepository.save(log);
        });
    }
}
