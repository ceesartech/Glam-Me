package tech.ceesar.glamme.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Helper methods for compatibility
    public double getLatitude() {
        return latitude != null ? latitude.doubleValue() : 0.0;
    }

    public void setLatitude(double latitude) {
        this.latitude = BigDecimal.valueOf(latitude);
    }

    public double getLongitude() {
        return longitude != null ? longitude.doubleValue() : 0.0;
    }

    public void setLongitude(double longitude) {
        this.longitude = BigDecimal.valueOf(longitude);
    }
}
