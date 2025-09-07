package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ride request from external providers
 */
@Data
@Builder
@NoArgsConstructor
public class RideRequest {
    private String rideId;
    private String customerId;
    private String driverId;
    private String status;
    private String driverName;
    private String driverPhone;
    private String vehicleModel;
    private String vehicleLicense;
    private Integer eta;

    // Explicit constructor for builder compatibility
    public RideRequest(String rideId, String customerId, String driverId, String status,
                      String driverName, String driverPhone, String vehicleModel,
                      String vehicleLicense, Integer eta) {
        this.rideId = rideId;
        this.customerId = customerId;
        this.driverId = driverId;
        this.status = status;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.vehicleModel = vehicleModel;
        this.vehicleLicense = vehicleLicense;
        this.eta = eta;
    }

    public static RideRequestBuilder builder() {
        return new RideRequestBuilder();
    }

    public static class RideRequestBuilder {
        private String rideId;
        private String customerId;
        private String driverId;
        private String status;
        private String driverName;
        private String driverPhone;
        private String vehicleModel;
        private String vehicleLicense;
        private Integer eta;

        public RideRequestBuilder rideId(String rideId) {
            this.rideId = rideId;
            return this;
        }

        public RideRequestBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public RideRequestBuilder driverId(String driverId) {
            this.driverId = driverId;
            return this;
        }

        public RideRequestBuilder status(String status) {
            this.status = status;
            return this;
        }

        public RideRequestBuilder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public RideRequestBuilder driverPhone(String driverPhone) {
            this.driverPhone = driverPhone;
            return this;
        }

        public RideRequestBuilder vehicleModel(String vehicleModel) {
            this.vehicleModel = vehicleModel;
            return this;
        }

        public RideRequestBuilder vehicleLicense(String vehicleLicense) {
            this.vehicleLicense = vehicleLicense;
            return this;
        }

        public RideRequestBuilder eta(Integer eta) {
            this.eta = eta;
            return this;
        }

        public RideRequest build() {
            return new RideRequest(rideId, customerId, driverId, status, driverName, driverPhone,
                    vehicleModel, vehicleLicense, eta);
        }
    }
}