package tech.ceesar.glamme.matching.client;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Grants a role to a user by calling the auth‑service.
 */
public interface AuthClient {
    /**
     * Grant the given role (e.g. "STYLIST") to the user.
     */
    Mono<Void> grantRole(UUID userId, String role);
}
