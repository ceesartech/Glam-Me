package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for detailed ride information from external providers
 */
@Data
@Builder
@NoArgsConstructor
public class RideDetails {
    private String rideId;
    private String status;
    private String driverName;
    private String driverPhone;
    private String vehicleModel;
    private String vehicleLicense;
    private Integer eta;

    // Explicit constructor for builder compatibility
    public RideDetails(String rideId, String status, String driverName,
                      String driverPhone, String vehicleModel, String vehicleLicense,
                      Integer eta) {
        this.rideId = rideId;
        this.status = status;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.vehicleModel = vehicleModel;
        this.vehicleLicense = vehicleLicense;
        this.eta = eta;
    }

    public static RideDetailsBuilder builder() {
        return new RideDetailsBuilder();
    }

    public static class RideDetailsBuilder {
        private String rideId;
        private String status;
        private String driverName;
        private String driverPhone;
        private String vehicleModel;
        private String vehicleLicense;
        private Integer eta;

        public RideDetailsBuilder rideId(String rideId) {
            this.rideId = rideId;
            return this;
        }

        public RideDetailsBuilder status(String status) {
            this.status = status;
            return this;
        }

        public RideDetailsBuilder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public RideDetailsBuilder driverPhone(String driverPhone) {
            this.driverPhone = driverPhone;
            return this;
        }

        public RideDetailsBuilder vehicleModel(String vehicleModel) {
            this.vehicleModel = vehicleModel;
            return this;
        }

        public RideDetailsBuilder vehicleLicense(String vehicleLicense) {
            this.vehicleLicense = vehicleLicense;
            return this;
        }

        public RideDetailsBuilder eta(Integer eta) {
            this.eta = eta;
            return this;
        }

        public RideDetails build() {
            return new RideDetails(rideId, status, driverName, driverPhone, vehicleModel, vehicleLicense, eta);
        }
    }
}
