package tech.ceesar.glamme.communication.service;

import tech.ceesar.glamme.communication.dto.*;

/**
 * Complete Communication Service Interface
 * Provides comprehensive communication functionality using AWS services
 */
public interface CommunicationService {

    // SMS functionality
    SendSmsResponse sendSms(SendSmsRequest request);
    BulkSmsResponse sendBulkSms(BulkSmsRequest request);

    // Push notifications
    PushResponse sendPushNotification(String deviceToken, String title, String body);
    PushResponse sendPushNotification(PushRequest request);

    // Email functionality
    EmailResponse sendEmail(String toAddress, String subject, String body);
    EmailResponse sendEmail(String toAddress, String subject, String body, String htmlBody);
    EmailResponse sendEmail(EmailRequest request);
    BulkEmailResponse sendBulkEmail(BulkEmailRequest request);

    // Video calling and meetings
    CallResponse initiateCall(CallRequest request);
    MeetingResponse createMeeting(MeetingDetails details);
    MeetingResponse joinMeeting(String meetingId, AttendeeInfo attendee);
    AttendeeResponse getAttendeeInfo(String meetingId, String attendeeId);
    CallResponse endCall(String callId);

    // Communication statistics
    SmsStats getSmsStats();
    EmailStats getEmailStats();

    // Health check
    String getHealthStatus();
}