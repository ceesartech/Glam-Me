package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Time slot response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponse {
    private Long id;
    private String stylistId;
    private LocalDateTime slotDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAvailable;
    private Long bookingId;
    private String serviceName;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
}
