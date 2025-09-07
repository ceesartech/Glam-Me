package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.StylistAvailability;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Availability response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private Long id;
    private String stylistId;
    private StylistAvailability.DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAvailable;
    private Integer slotDurationMinutes;
    private List<String> availableServices;
    private Integer maxBookingsPerSlot;
    private Integer breakDurationMinutes;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
