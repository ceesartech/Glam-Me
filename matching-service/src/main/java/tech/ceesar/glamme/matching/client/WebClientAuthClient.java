package tech.ceesar.glamme.matching.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebClientAuthClient implements AuthClient {

    private final WebClient authWebClient;

    @Override
    public Mono<Void> grantRole(UUID userId, String role) {
        return authWebClient
                .put()
                .uri(uri -> uri
                        .path("/api/admin/users/{userId}/roles")
                        .queryParam("role", role)
                        .build(userId))
                .retrieve()
                .toBodilessEntity()
                .then();  // convert to Mono<Void>
    }
}
