package tech.ceesar.glamme.communication.client;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class TwilioClientImpl implements TwilioClient {
    private final String fromNumber;
    private final String baseUrl; // e.g. https://myapp.com

    @Override
    public Message sendSms(String to, String body) {
        return Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(fromNumber),
                body
        ).create();
    }

    @Override
    public Call createCall(String to) {
        // TwiML endpoint will receive ?to=... and dial that number
        String twimlUrl = String.format("%s/communications/voice/twiml?to=%s",
                baseUrl,
                URLEncoder.encode(to, StandardCharsets.UTF_8)
        );
        return Call.creator(
                new PhoneNumber(to),
                new PhoneNumber(fromNumber),
                URI.create(twimlUrl)
        ).create();
    }
}
