package tech.ceesar.glamme.communication.client;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;

public interface TwilioClient {
    /**
     * Send an SMS via Twilio.
     */
    Message sendSms(String to, String body);

    /**
     * Initiate a voice call via Twilio, connecting to 'to' number.
     */
    Call createCall(String to);
}
