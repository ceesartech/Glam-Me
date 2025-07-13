package tech.ceesar.glamme.ride.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.ceesar.glamme.ride.dto.CreateRideRequest;
import tech.ceesar.glamme.ride.dto.RideStatusResponse;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;

import java.util.List;
import java.util.Map;

/**
 * Stub integration with Uber Rides API.
 */
@Component("UBER")
public class UberRideClient implements ExternalRideClient {
    private final WebClient http;

    public UberRideClient(@Qualifier("UBER") WebClient http) {
        this.http = http;
    }

    @Override
    public String requestRide(CreateRideRequest req) {
        // 1) fetch products at pickup
        var productsResp = http.get()
                .uri(uri -> uri.path("/products")
                        .queryParam("latitude", req.getPickupLocation().getLatitude())
                        .queryParam("longitude", req.getPickupLocation().getLongitude())
                        .build())
                .retrieve()
                .bodyToMono(Map.class).block();
        List<Map<String,Object>> prods = (List<Map<String,Object>>)productsResp.get("products");
        String productId = (String)prods.get(0).get("product_id");

        // 2) create ride
        Map<String,Object> body = Map.of(
                "product_id", productId,
                "start_latitude",  req.getPickupLocation().getLatitude(),
                "start_longitude", req.getPickupLocation().getLongitude(),
                "end_latitude",    req.getDropoffLocation().getLatitude(),
                "end_longitude",   req.getDropoffLocation().getLongitude()
        );
        var rideResp = http.post()
                .uri("/requests")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class).block();
        return (String)rideResp.get("request_id");
    }

    @Override
    public RideStatusResponse getStatus(String externalRideId) {
        var resp = http.get()
                .uri("/requests/{id}", externalRideId)
                .retrieve()
                .bodyToMono(Map.class).block();
        String status = (String)resp.get("status"); // e.g. "accepted"
        return new RideStatusResponse(
                null, ProviderType.UBER,
                RideStatus.valueOf(status.toUpperCase()),
                externalRideId, null
        );
    }

    @Override
    public boolean cancelRide(String externalRideId) {
        http.delete()
                .uri("/requests/{id}", externalRideId)
                .retrieve().toBodilessEntity().block();
        return true;
    }
}
