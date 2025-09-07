package tech.ceesar.glamme.ride.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String driverId;
    private LocationDto currentLocation;
    private Integer heading;
    private BigDecimal speedMph;
    private BigDecimal accuracyMeters;
    private Integer etaToDestination;
    private BigDecimal distanceTraveled;
    private LocalDateTime timestamp;
    private String status;

    // Legacy fields for compatibility
    private BigDecimal driverLatitude;
    private BigDecimal driverLongitude;
    private Integer driverHeading;
    private Integer estimatedArrivalMinutes;
    private String trackingData;
    private LocalDateTime createdAt;

    // Helper methods
    public static RideTrackingDtoBuilder builder() {
        return new RideTrackingDtoBuilder();
    }

    public static class RideTrackingDtoBuilder {
        private Long id;
        private String rideId;
        private String driverId;
        private LocationDto currentLocation;
        private Integer heading;
        private BigDecimal speedMph;
        private BigDecimal accuracyMeters;
        private Integer etaToDestination;
        private BigDecimal distanceTraveled;
        private LocalDateTime timestamp;
        private String status;

        public RideTrackingDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RideTrackingDtoBuilder rideId(String rideId) {
            this.rideId = rideId;
            return this;
        }

        public RideTrackingDtoBuilder driverId(String driverId) {
            this.driverId = driverId;
            return this;
        }

        public RideTrackingDtoBuilder currentLocation(LocationDto currentLocation) {
            this.currentLocation = currentLocation;
            return this;
        }

        public RideTrackingDtoBuilder heading(Integer heading) {
            this.heading = heading;
            return this;
        }

        public RideTrackingDtoBuilder speedMph(BigDecimal speedMph) {
            this.speedMph = speedMph;
            return this;
        }

        public RideTrackingDtoBuilder accuracyMeters(BigDecimal accuracyMeters) {
            this.accuracyMeters = accuracyMeters;
            return this;
        }

        public RideTrackingDtoBuilder etaToDestination(Integer etaToDestination) {
            this.etaToDestination = etaToDestination;
            return this;
        }

        public RideTrackingDtoBuilder distanceTraveled(BigDecimal distanceTraveled) {
            this.distanceTraveled = distanceTraveled;
            return this;
        }

        public RideTrackingDtoBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RideTrackingDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public RideTrackingDto build() {
            return new RideTrackingDto(id, rideId, driverId, currentLocation, heading, speedMph,
                    accuracyMeters, etaToDestination, distanceTraveled, timestamp, status,
                    driverLatitude, driverLongitude, driverHeading, estimatedArrivalMinutes,
                    trackingData, createdAt);
        }
    }
}
