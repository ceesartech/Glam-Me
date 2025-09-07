package tech.ceesar.glamme.ride.dto;

import com.stripe.model.Review;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateRideRequest {
    private UUID customerId;

    private UUID bookingId;                // optional

    private LocationDto pickupLocation;

    private LocationDto dropoffLocation;

    private ProviderType providerType;

    private String productId;              // optional

    // Helper methods for compatibility
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
