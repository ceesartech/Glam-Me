package tech.ceesar.glamme.communication.config;

import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.ceesar.glamme.communication.client.TwilioClient;
import tech.ceesar.glamme.communication.client.TwilioClientImpl;

@Configuration
public class TwilioConfig {
    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Bean
    public TwilioClient twilioClient(@Value("${twilio.fromNumber}") String fromNumber,
                                     @Value("${twilio.baseUrl}") String baseUrl) {
        Twilio.init(accountSid, authToken);
        return new TwilioClientImpl(fromNumber, baseUrl);
    }
}
