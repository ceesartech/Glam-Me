package tech.ceesar.glamme.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingRequest {
    
    @NotBlank(message = "Stylist ID is required")
    private String stylistId;
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    private String serviceDescription;
    
    @NotNull(message = "Appointment date is required")
    private LocalDateTime appointmentDate;
    
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    private String notes;
    private String specialRequests;
    private Set<String> addons;
    
    @NotNull(message = "Location type is required")
    private Booking.LocationType locationType;
    
    private String locationAddress;
    private BigDecimal locationLatitude;
    private BigDecimal locationLongitude;
}