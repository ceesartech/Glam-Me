package tech.ceesar.glamme.ride.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.ceesar.glamme.ride.dto.CreateRideRequest;
import tech.ceesar.glamme.ride.dto.RideStatusResponse;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;

import java.util.Map;

@Component("LYFT")
public class LyftRideClient implements ExternalRideClient {
    private final WebClient http;

    public LyftRideClient(@Qualifier("LYFT") WebClient http) {
        this.http = http;
    }

    private String getAccessToken() {
        Map<String,Object> resp = http.post()
                .uri("https://api.lyft.com/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("grant_type","client_credentials","scope","rides.read rides.request"))
                .retrieve()
                .bodyToMono(Map.class).block();
        return (String)resp.get("access_token");
    }

    @Override
    public String requestRide(CreateRideRequest req) {
        String bearer = "Bearer " + getAccessToken();
        WebClient client = http.mutate().defaultHeader("Authorization", bearer).build();
        Map<String,Object> body = Map.of(
                "ride_type","lyft",
                "origin", Map.of(
                        "lat", req.getPickupLocation().getLatitude(),
                        "lng", req.getPickupLocation().getLongitude()
                ),
                "destination", Map.of(
                        "lat", req.getPickupLocation().getLatitude(),
                        "lng", req.getPickupLocation().getLongitude()
                )
        );
        var resp = client.post().uri("/rides")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class).block();
        return (String)((Map)resp.get("ride")).get("id");
    }

    @Override
    public RideStatusResponse getStatus(String externalRideId) {
        String token = getAccessToken();
        WebClient client = http.mutate().defaultHeader("Authorization", "Bearer "+token).build();
        var resp = client.get().uri("/rides/{id}", externalRideId)
                .retrieve().bodyToMono(Map.class).block();
        String status = (String) ((Map)resp.get("ride")).get("status");
        return new RideStatusResponse(
                null, ProviderType.LYFT,
                RideStatus.valueOf(status.toUpperCase()), externalRideId, null
        );
    }

    @Override
    public boolean cancelRide(String externalRideId) {
        String token = getAccessToken();
        WebClient client = http.mutate().defaultHeader("Authorization", "Bearer "+token).build();
        client.post().uri("/rides/{id}/cancel", externalRideId)
                .retrieve().toBodilessEntity().block();
        return true;
    }
}
