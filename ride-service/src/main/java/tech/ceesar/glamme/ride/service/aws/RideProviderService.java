package tech.ceesar.glamme.ride.service.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.ride.dto.LocationDto;
import tech.ceesar.glamme.ride.enums.ProviderType;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideProviderService {

    private final SecretsManagerClient secretsManagerClient;
    private final EventPublisher eventPublisher;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.secrets.uber-arn:}")
    private String uberSecretArn;

    @Value("${aws.secrets.lyft-arn:}")
    private String lyftSecretArn;

    @Value("${ride.providers.uber.base-url:https://api.uber.com}")
    private String uberBaseUrl;

    @Value("${ride.providers.lyft.base-url:https://api.lyft.com}")
    private String lyftBaseUrl;

    /**
     * Get ride estimates from Uber
     */
    public Mono<RideEstimate> getUberEstimate(LocationDto pickup, LocationDto dropoff) {
        return getUberCredentials()
                .flatMap(credentials -> {
                    String serverToken = credentials.get("server_token").asText();

                    return webClient.get()
                            .uri(uberBaseUrl + "/v1.2/estimates/price")
                            .header("Authorization", "Token " + serverToken)
                            .header("Accept-Language", "en_US")
                            .header("Content-Type", "application/json")
                            .attribute("start_latitude", pickup.getLatitude())
                            .attribute("start_longitude", pickup.getLongitude())
                            .attribute("end_latitude", dropoff.getLatitude())
                            .attribute("end_longitude", dropoff.getLongitude())
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseUberEstimate)
                            .doOnSuccess(estimate -> log.info("Retrieved Uber estimate: {}", estimate))
                            .doOnError(error -> {
                                log.error("Failed to get Uber estimate", error);
                                eventPublisher.publishEvent("ride.provider.error", Map.of(
                                        "provider", "UBER",
                                        "operation", "ESTIMATE",
                                        "error", error.getMessage()
                                ));
                            });
                });
    }

    /**
     * Get ride estimates from Lyft
     */
    public Mono<RideEstimate> getLyftEstimate(LocationDto pickup, LocationDto dropoff) {
        return getLyftCredentials()
                .flatMap(credentials -> {
                    String accessToken = getLyftAccessToken(credentials);

                    return webClient.get()
                            .uri(lyftBaseUrl + "/v1/cost")
                            .header("Authorization", "Bearer " + accessToken)
                            .attribute("start_lat", pickup.getLatitude())
                            .attribute("start_lng", pickup.getLongitude())
                            .attribute("end_lat", dropoff.getLatitude())
                            .attribute("end_lng", dropoff.getLongitude())
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseLyftEstimate)
                            .doOnSuccess(estimate -> log.info("Retrieved Lyft estimate: {}", estimate))
                            .doOnError(error -> {
                                log.error("Failed to get Lyft estimate", error);
                                eventPublisher.publishEvent("ride.provider.error", Map.of(
                                        "provider", "LYFT",
                                        "operation", "ESTIMATE",
                                        "error", error.getMessage()
                                ));
                            });
                });
    }

    /**
     * Request ride from Uber
     */
    public Mono<RideRequest> requestUberRide(RideBookingRequest request) {
        return getUberCredentials()
                .flatMap(credentials -> {
                    String accessToken = getUberAccessToken(credentials);

                    Map<String, Object> rideRequest = Map.of(
                            "start_latitude", request.getPickupLocation().getLatitude(),
                            "start_longitude", request.getPickupLocation().getLongitude(),
                            "end_latitude", request.getDropoffLocation().getLatitude(),
                            "end_longitude", request.getDropoffLocation().getLongitude(),
                            "product_id", request.getProductId(),
                            "surge_confirmation_id", request.getSurgeConfirmationId()
                    );

                    return webClient.post()
                            .uri(uberBaseUrl + "/v1.2/requests")
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(rideRequest)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseUberRideRequest)
                            .doOnSuccess(ride -> {
                                log.info("Requested Uber ride: {}", ride.getRideId());
                                eventPublisher.publishEvent("ride.requested", Map.of(
                                        "provider", "UBER",
                                        "rideId", ride.getRideId(),
                                        "customerId", request.getCustomerId()
                                ));
                            })
                            .doOnError(error -> {
                                log.error("Failed to request Uber ride", error);
                                eventPublisher.publishEvent("ride.request.failed", Map.of(
                                        "provider", "UBER",
                                        "customerId", request.getCustomerId(),
                                        "error", error.getMessage()
                                ));
                            });
                });
    }

    /**
     * Request ride from Lyft
     */
    public Mono<RideRequest> requestLyftRide(RideBookingRequest request) {
        return getLyftCredentials()
                .flatMap(credentials -> {
                    String accessToken = getLyftAccessToken(credentials);

                    Map<String, Object> rideRequest = Map.of(
                            "ride_type", request.getProductId(),
                            "origin", Map.of(
                                    "lat", request.getPickupLocation().getLatitude(),
                                    "lng", request.getPickupLocation().getLongitude()
                            ),
                            "destination", Map.of(
                                    "lat", request.getDropoffLocation().getLatitude(),
                                    "lng", request.getDropoffLocation().getLongitude()
                            )
                    );

                    return webClient.post()
                            .uri(lyftBaseUrl + "/v1/rides")
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(rideRequest)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseLyftRideRequest)
                            .doOnSuccess(ride -> {
                                log.info("Requested Lyft ride: {}", ride.getRideId());
                                eventPublisher.publishEvent("ride.requested", Map.of(
                                        "provider", "LYFT",
                                        "rideId", ride.getRideId(),
                                        "customerId", request.getCustomerId()
                                ));
                            })
                            .doOnError(error -> {
                                log.error("Failed to request Lyft ride", error);
                                eventPublisher.publishEvent("ride.request.failed", Map.of(
                                        "provider", "LYFT",
                                        "customerId", request.getCustomerId(),
                                        "error", error.getMessage()
                                ));
                            });
                });
    }

    /**
     * Cancel ride with provider
     */
    public Mono<Void> cancelRide(String provider, String rideId) {
        if ("UBER".equals(provider)) {
            return cancelUberRide(rideId);
        } else if ("LYFT".equals(provider)) {
            return cancelLyftRide(rideId);
        }
        return Mono.error(new IllegalArgumentException("Unknown provider: " + provider));
    }

    private Mono<Void> cancelUberRide(String rideId) {
        return getUberCredentials()
                .flatMap(credentials -> {
                    String accessToken = getUberAccessToken(credentials);

                    return webClient.patch()
                            .uri(uberBaseUrl + "/v1.2/requests/" + rideId)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .bodyValue(Map.of("status", "rider_cancelled"))
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v -> {
                                log.info("Cancelled Uber ride: {}", rideId);
                                eventPublisher.publishEvent("ride.cancelled", Map.of(
                                        "provider", "UBER",
                                        "rideId", rideId
                                ));
                            });
                });
    }

    private Mono<Void> cancelLyftRide(String rideId) {
        return getLyftCredentials()
                .flatMap(credentials -> {
                    String accessToken = getLyftAccessToken(credentials);

                    return webClient.post()
                            .uri(lyftBaseUrl + "/v1/rides/" + rideId + "/cancel")
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v -> {
                                log.info("Cancelled Lyft ride: {}", rideId);
                                eventPublisher.publishEvent("ride.cancelled", Map.of(
                                        "provider", "LYFT",
                                        "rideId", rideId
                                ));
                            });
                });
    }

    /**
     * Get ride details from provider
     */
    public Mono<RideDetails> getRideDetails(String provider, String rideId) {
        if ("UBER".equals(provider)) {
            return getUberRideDetails(rideId);
        } else if ("LYFT".equals(provider)) {
            return getLyftRideDetails(rideId);
        }
        return Mono.error(new IllegalArgumentException("Unknown provider: " + provider));
    }

    private Mono<RideDetails> getUberRideDetails(String rideId) {
        return getUberCredentials()
                .flatMap(credentials -> {
                    String accessToken = getUberAccessToken(credentials);

                    return webClient.get()
                            .uri(uberBaseUrl + "/v1.2/requests/" + rideId)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseUberRideDetails);
                });
    }

    private Mono<RideDetails> getLyftRideDetails(String rideId) {
        return getLyftCredentials()
                .flatMap(credentials -> {
                    String accessToken = getLyftAccessToken(credentials);

                    return webClient.get()
                            .uri(lyftBaseUrl + "/v1/rides/" + rideId)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseLyftRideDetails);
                });
    }

    // Helper methods for credentials
    private Mono<JsonNode> getUberCredentials() {
        return Mono.fromCallable(() -> {
            if (uberSecretArn == null || uberSecretArn.isEmpty()) {
                throw new RuntimeException("Uber secret ARN not configured");
            }

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(uberSecretArn)
                    .build();

            var response = secretsManagerClient.getSecretValue(request);
            return objectMapper.readTree(response.secretString());
        });
    }

    private Mono<JsonNode> getLyftCredentials() {
        return Mono.fromCallable(() -> {
            if (lyftSecretArn == null || lyftSecretArn.isEmpty()) {
                throw new RuntimeException("Lyft secret ARN not configured");
            }

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(lyftSecretArn)
                    .build();

            var response = secretsManagerClient.getSecretValue(request);
            return objectMapper.readTree(response.secretString());
        });
    }

    private String getUberAccessToken(JsonNode credentials) {
        // In a real implementation, you would handle token refresh
        return credentials.get("access_token").asText();
    }

    private String getLyftAccessToken(JsonNode credentials) {
        // In a real implementation, you would handle token refresh
        return credentials.get("access_token").asText();
    }

    // Parsing methods (simplified for demonstration)
    private RideEstimate parseUberEstimate(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            JsonNode price = json.get("prices").get(0);
            return RideEstimate.builder()
                    .provider(ProviderType.UBER)
                    .productId(price.get("product_id").asText())
                    .displayName(price.get("display_name").asText())
                    .estimate(price.get("estimate").asText())
                    .distance(price.get("distance").asDouble())
                    .duration(price.get("duration").asInt())
                    .currencyCode("USD")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Uber estimate", e);
            throw new RuntimeException("Failed to parse Uber estimate", e);
        }
    }

    private RideEstimate parseLyftEstimate(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            JsonNode cost = json.get("cost_estimates").get(0);
            return RideEstimate.builder()
                    .provider(ProviderType.LYFT)
                    .productId(cost.get("ride_type").asText())
                    .displayName(cost.get("display_name").asText())
                    .estimate("$" + cost.get("estimated_cost_cents_min").asInt()/100 + "-" +
                             "$" + cost.get("estimated_cost_cents_max").asInt()/100)
                    .distance(cost.get("estimated_distance_miles").asDouble())
                    .duration(cost.get("estimated_duration_seconds").asInt())
                    .currencyCode("USD")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Lyft estimate", e);
            throw new RuntimeException("Failed to parse Lyft estimate", e);
        }
    }

    private RideRequest parseUberRideRequest(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            return RideRequest.builder()
                    .rideId(json.get("request_id").asText())
                    .status(json.get("status").asText())
                    .provider(ProviderType.UBER)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Uber ride request", e);
            throw new RuntimeException("Failed to parse Uber ride request", e);
        }
    }

    private RideRequest parseLyftRideRequest(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            return RideRequest.builder()
                    .rideId(json.get("ride_id").asText())
                    .status(json.get("status").asText())
                    .provider(ProviderType.LYFT)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Lyft ride request", e);
            throw new RuntimeException("Failed to parse Lyft ride request", e);
        }
    }

    private RideDetails parseUberRideDetails(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            return RideDetails.builder()
                    .rideId(json.get("request_id").asText())
                    .status(json.get("status").asText())
                    .driverName(json.get("driver") != null ? json.get("driver").get("name").asText() : null)
                    .driverPhone(json.get("driver") != null ? json.get("driver").get("phone_number").asText() : null)
                    .vehicleModel(json.get("vehicle") != null ? json.get("vehicle").get("model").asText() : null)
                    .vehicleLicense(json.get("vehicle") != null ? json.get("vehicle").get("license_plate").asText() : null)
                    .eta(json.get("pickup") != null ? json.get("pickup").get("eta").asInt() : null)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Uber ride details", e);
            throw new RuntimeException("Failed to parse Uber ride details", e);
        }
    }

    private RideDetails parseLyftRideDetails(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            return RideDetails.builder()
                    .rideId(json.get("ride_id").asText())
                    .status(json.get("status").asText())
                    .driverName(json.get("driver") != null ? json.get("driver").get("first_name").asText() + " " +
                              json.get("driver").get("last_name").asText() : null)
                    .driverPhone(json.get("driver") != null ? json.get("driver").get("phone_number").asText() : null)
                    .vehicleModel(json.get("vehicle") != null ? json.get("vehicle").get("model").asText() : null)
                    .vehicleLicense(json.get("vehicle") != null ? json.get("vehicle").get("license_plate").asText() : null)
                    .eta(json.get("pickup") != null ? json.get("pickup").get("time_to_pickup").asInt() : null)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Lyft ride details", e);
            throw new RuntimeException("Failed to parse Lyft ride details", e);
        }
    }

    // DTOs
    public record RideBookingRequest(String customerId, LocationDto pickupLocation,
                                   LocationDto dropoffLocation, String productId,
                                   String surgeConfirmationId) {}

    public record RideEstimate(ProviderType provider, String productId, String displayName,
                             String estimate, Double distance, Integer duration, String currencyCode) {}

    public record RideRequest(String rideId, String status, ProviderType provider) {}

    public record RideDetails(String rideId, String status, String driverName, String driverPhone,
                            String vehicleModel, String vehicleLicense, Integer eta) {}
}
