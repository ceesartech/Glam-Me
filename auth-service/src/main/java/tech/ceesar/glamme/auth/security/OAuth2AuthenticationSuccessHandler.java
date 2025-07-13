package tech.ceesar.glamme.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tech.ceesar.glamme.auth.entity.User;
import tech.ceesar.glamme.auth.repository.UserRepository;
import tech.ceesar.glamme.common.enums.Role;
import tech.ceesar.glamme.common.enums.SubscriptionType;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();   // Google, Facebook, Apple, Amazon
        String providerId = oauthUser.getAttribute("sub");  // For Google
        if (providerId == null) {
            providerId = oauthUser.getName();   // default
        }
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        String finalProviderId = providerId;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .name(name)
                    .email(email)
                    .roles(Set.of(Role.CUSTOMER))
                    .subscriptionType(SubscriptionType.FREE)
                    .oauthProvider(provider)
                    .oauthProviderId(finalProviderId)
                    .createdAt(Instant.now())
                    .build();

            return userRepository.save(newUser);
        });

        String token = tokenProvider.generateToken(user.getId());
        String redirectUri = getRedirectUri(token);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String getRedirectUri(String token) {

        return String.format("%s?token=%s",
                System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:3000"), token
        );
    }
}
