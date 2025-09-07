package tech.ceesar.glamme.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.common.enums.Role;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Authentication result containing user information and tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private String userId;
    private String username;
    private String email;
    private String accessToken;
    private String refreshToken;
    private String idToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Set<Role> roles;
    private String subscriptionType;
    private boolean emailVerified;
    private boolean accountEnabled;

    /**
     * Check if the token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if the token is still valid
     */
    public boolean isValid() {
        return !isExpired() && accessToken != null && !accessToken.isEmpty();
    }
}
