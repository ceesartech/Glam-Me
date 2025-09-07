package tech.ceesar.glamme.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.StylistAvailability;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylistAvailabilityDto {
    
    @NotBlank(message = "Stylist ID is required")
    private String stylistId;
    
    @NotNull(message = "Day of week is required")
    private StylistAvailability.DayOfWeek dayOfWeek;
    
    @NotBlank(message = "Start time is required")
    private String startTime; // HH:mm format
    
    @NotBlank(message = "End time is required")
    private String endTime; // HH:mm format
    
    @NotNull(message = "Availability status is required")
    private Boolean isAvailable;
    
    private Integer maxBookingsPerSlot;
    
    private Integer breakDurationMinutes;
    
    private Set<StylistAvailability.Service> availableServices;
}
