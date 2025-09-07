package tech.ceesar.glamme.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.auth.entity.User;
import tech.ceesar.glamme.auth.repository.UserRepository;
import tech.ceesar.glamme.common.enums.Role;
import tech.ceesar.glamme.common.enums.SubscriptionType;
import tech.ceesar.glamme.common.service.RedisCacheService;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final UserRepository userRepository;
    private final CognitoAuthService cognitoAuthService;
    private final RedisCacheService cacheService;

    /**
     * Sync user data from Cognito to local database
     */
    @Transactional
    public User syncUserFromCognito(String cognitoSub, String username) {
        try {
            // Get user from Cognito
            var cognitoUser = cognitoAuthService.getUser(username);

            // Check if user exists locally
            User localUser = userRepository.findByCognitoSub(cognitoSub)
                    .orElse(null);

            if (localUser == null) {
                // Create new user
                localUser = createUserFromCognito(cognitoUser, cognitoSub);
                log.info("Created new user from Cognito: {}", username);
            } else {
                // Update existing user
                updateUserFromCognito(localUser, cognitoUser);
                log.info("Updated user from Cognito: {}", username);
            }

            return userRepository.save(localUser);

        } catch (Exception e) {
            log.error("Failed to sync user {} from Cognito", username, e);
            throw new RuntimeException("Failed to sync user from Cognito", e);
        }
    }

    private User createUserFromCognito(software.amazon.awssdk.services.cognitoidentityprovider.model.UserType cognitoUser, String cognitoSub) {
        Map<String, String> attributes = extractUserAttributes(cognitoUser);

        Set<Role> roles = new HashSet<>();
        String userType = attributes.getOrDefault("user_type", "customer");

        // Map user type to roles
        switch (userType.toLowerCase()) {
            case "stylist":
                roles.add(Role.ROLE_STYLIST);
                break;
            case "admin":
                roles.add(Role.ROLE_ADMIN);
                break;
            default:
                roles.add(Role.ROLE_USER);
                break;
        }

        return User.builder()
                .cognitoSub(cognitoSub)
                .username(cognitoUser.username())
                .name(attributes.get("name"))
                .email(attributes.get("email"))
                .userType(userType)
                .plan(attributes.getOrDefault("plan", "FREE"))
                .roles(roles)
                .subscriptionType(SubscriptionType.valueOf(attributes.getOrDefault("plan", "FREE")))
                .enabled(cognitoUser.enabled())
                .emailVerified(Boolean.parseBoolean(attributes.getOrDefault("email_verified", "false")))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void updateUserFromCognito(User localUser, software.amazon.awssdk.services.cognitoidentityprovider.model.UserType cognitoUser) {
        Map<String, String> attributes = extractUserAttributes(cognitoUser);

        localUser.setUsername(cognitoUser.username());
        localUser.setName(attributes.get("name"));
        localUser.setEmail(attributes.get("email"));
        localUser.setUserType(attributes.getOrDefault("user_type", "customer"));
        localUser.setPlan(attributes.getOrDefault("plan", "FREE"));
        localUser.setEnabled(cognitoUser.enabled());
        localUser.setEmailVerified(Boolean.parseBoolean(attributes.getOrDefault("email_verified", "false")));
        localUser.setUpdatedAt(Instant.now());

        // Update subscription type
        try {
            localUser.setSubscriptionType(SubscriptionType.valueOf(attributes.getOrDefault("plan", "FREE")));
        } catch (IllegalArgumentException e) {
            localUser.setSubscriptionType(SubscriptionType.FREE);
        }

        // Update roles based on user type
        Set<Role> roles = new HashSet<>();
        String userType = attributes.getOrDefault("user_type", "customer");
        switch (userType.toLowerCase()) {
            case "stylist":
                roles.add(Role.ROLE_STYLIST);
                break;
            case "admin":
                roles.add(Role.ROLE_ADMIN);
                break;
            default:
                roles.add(Role.ROLE_USER);
                break;
        }
        localUser.setRoles(roles);
    }

    private Map<String, String> extractUserAttributes(software.amazon.awssdk.services.cognitoidentityprovider.model.UserType cognitoUser) {
        return cognitoUser.attributes().stream()
                .collect(java.util.stream.Collectors.toMap(
                        attr -> attr.name(),
                        attr -> attr.value()
                ));
    }

    /**
     * Get or create user from Cognito sub
     */
    public User getOrCreateUser(String cognitoSub, String username) {
        // Try to get from cache first
        Optional<User> cachedUser = cacheService.get("user:local:" + cognitoSub, User.class);
        if (cachedUser.isPresent()) {
            log.debug("Retrieved user {} from cache", cognitoSub);
            return cachedUser.get();
        }

        // Get from database or sync from Cognito
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseGet(() -> syncUserFromCognito(cognitoSub, username));

        // Cache the user data
        cacheService.set("user:local:" + cognitoSub, user, Duration.ofHours(1));

        return user;
    }

    /**
     * Update user profile in both Cognito and local database
     */
    @Transactional
    public void updateUserProfile(String cognitoSub, Map<String, String> attributes) {
        // Update in Cognito
        String username = userRepository.findByCognitoSub(cognitoSub)
                .map(User::getUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        cognitoAuthService.updateUserAttributes(username, attributes);

        // Sync back to local database
        syncUserFromCognito(cognitoSub, username);
    }
}
