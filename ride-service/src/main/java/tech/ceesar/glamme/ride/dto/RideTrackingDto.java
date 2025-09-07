package tech.ceesar.glamme.ride.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.ride.entity.RideTracking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideTrackingDto {
    
    private Long id;
    
    private String rideId;
    
    private BigDecimal driverLatitude;
    
    private BigDecimal driverLongitude;
    
    private Integer driverHeading;
    
    private Integer estimatedArrivalMinutes;
    
    private String status;
    
    private String trackingData;
    
    private LocalDateTime createdAt;
}
