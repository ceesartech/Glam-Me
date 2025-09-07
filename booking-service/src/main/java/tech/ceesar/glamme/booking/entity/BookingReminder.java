package tech.ceesar.glamme.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingReminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_id", nullable = false)
    private String bookingId;
    
    @Column(name = "reminder_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReminderType reminderType;
    
    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivery_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;
    
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Column(name = "recipient_phone")
    private String recipientPhone;
    
    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum ReminderType {
        BOOKING_CONFIRMATION, REMINDER_24H, REMINDER_2H, CANCELLATION_NOTICE, COMPLETION_FOLLOWUP
    }
    
    public enum Status {
        PENDING, SENT, FAILED, CANCELLED
    }
    
    public enum DeliveryMethod {
        EMAIL, SMS, PUSH_NOTIFICATION, IN_APP
    }
}
