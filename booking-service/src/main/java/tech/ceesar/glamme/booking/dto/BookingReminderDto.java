package tech.ceesar.glamme.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.BookingReminder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingReminderDto {
    
    private Long id;
    
    @NotBlank(message = "Booking ID is required")
    private String bookingId;
    
    @NotNull(message = "Reminder type is required")
    private BookingReminder.ReminderType reminderType;
    
    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;
    
    private BookingReminder.Status status;
    
    private LocalDateTime sentAt;
    
    @NotNull(message = "Delivery method is required")
    private BookingReminder.DeliveryMethod deliveryMethod;
    
    private String recipientEmail;
    
    private String recipientPhone;
    
    private String messageContent;
    
    private String errorMessage;
    
    private Integer retryCount;
    
    private LocalDateTime createdAt;
}
