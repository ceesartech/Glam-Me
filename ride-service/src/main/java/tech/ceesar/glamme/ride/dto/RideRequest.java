package tech.ceesar.glamme.ride.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.ride.entity.Ride;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideRequest {
    
    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;
    
    @NotNull(message = "Pickup latitude is required")
    private BigDecimal pickupLatitude;
    
    @NotNull(message = "Pickup longitude is required")
    private BigDecimal pickupLongitude;
    
    @NotBlank(message = "Dropoff address is required")
    private String dropoffAddress;
    
    @NotNull(message = "Dropoff latitude is required")
    private BigDecimal dropoffLatitude;
    
    @NotNull(message = "Dropoff longitude is required")
    private BigDecimal dropoffLongitude;
    
    private Ride.Provider preferredProvider;
    private String notes;
}