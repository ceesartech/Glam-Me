package tech.ceesar.glamme.communication.service;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CommunicationServiceTest {
    @Mock
    TwilioClient twilioClient;
    @Mock
    CommunicationLogRepository logRepo;
    @InjectMocks
    CommunicationService service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendSms_logsAndReturns() {
        // arrange
        SendSmsRequest req = new SendSmsRequest();
        req.setFromNumber("+1000000000");
        req.setToNumber("+1111111111");
        req.setMessage("Hi");

        Message mockMsg = mock(Message.class);
        when(mockMsg.getSid()).thenReturn("MSG123");
        when(mockMsg.getStatus()).thenReturn(Message.Status.QUEUED);
        when(twilioClient.sendSms(anyString(), anyString())).thenReturn(mockMsg);
        when(logRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // act
        SendSmsResponse resp = service.sendSms(req);

        // assert
        assert resp.getMessageSid().equals("MSG123");
        verify(logRepo).save(argThat(log ->
                log.getChannel() == Channel.SMS &&
                        log.getDirection() == Direction.OUTBOUND &&
                        "Hi".equals(log.getMessageBody()) &&
                        "MSG123".equals(log.getSid())
        ));
    }

    @Test
    void initiateCall_logsAndReturns() {
        Call mockCall = mock(Call.class);
        when(mockCall.getSid()).thenReturn("CALL123");
        when(mockCall.getStatus()).thenReturn(Call.Status.QUEUED);
        when(twilioClient.createCall("+1222222222")).thenReturn(mockCall);
        when(logRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        CallRequest req = new CallRequest();
        req.setFromNumber("+1000000000");
        req.setToNumber("+1222222222");

        CallResponse resp = service.initiateCall(req);
        assert resp.getCallSid().equals("CALL123");
        verify(logRepo).save(argThat(log ->
                log.getChannel() == Channel.VOICE &&
                        log.getDirection() == Direction.OUTBOUND &&
                        "CALL123".equals(log.getSid())
        ));
    }

    @Test
    void handleSmsStatusCallback_updatesStatus() {
        CommunicationLog existing = CommunicationLog.builder()
                .sid("MSG123").status("queued").build();
        when(logRepo.findBySid("MSG123"))
                .thenReturn(Optional.of(existing));
        service.handleSmsStatusCallback("MSG123", "delivered");
        verify(logRepo).save(argThat(log -> "delivered".equals(log.getStatus())));
    }

    @Test
    void handleIncomingSms_savesInbound() {
        when(logRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        service.handleIncomingSms("+1", "+2", "Hello", "MSG456");
        verify(logRepo).save(argThat(log ->
                log.getDirection() == Direction.INBOUND &&
                        log.getChannel() == Channel.SMS &&
                        "Hello".equals(log.getMessageBody()) &&
                        "MSG456".equals(log.getSid())
        ));
    }

    @Test
    void handleCallStatusCallback_updatesStatus() {
        CommunicationLog existing = CommunicationLog.builder()
                .sid("CALL123").status("queued").build();
        when(logRepo.findBySid("CALL123"))
                .thenReturn(Optional.of(existing));
        service.handleCallStatusCallback("CALL123", "completed");
        verify(logRepo).save(argThat(log -> "completed".equals(log.getStatus())));
    }
}
