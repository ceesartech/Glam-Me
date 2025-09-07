package tech.ceesar.glamme.communication.client;

/**
 * AWS-based communication client interface
 * Replaces Twilio with native AWS services (SES, SNS, Pinpoint)
 */
public interface AwsCommunicationClient {

    /**
     * Send email via AWS SES
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Send SMS via AWS SNS
     */
    void sendSms(String phoneNumber, String message);

    /**
     * Send push notification via AWS Pinpoint
     */
    void sendPushNotification(String deviceToken, String title, String body);

    /**
     * Send booking confirmation (email + SMS)
     */
    void sendBookingConfirmation(String email, String phone, String bookingDetails);

    /**
     * Send match notification (email + SMS)
     */
    void sendMatchNotification(String email, String phone, String stylistName, String serviceType);
}
