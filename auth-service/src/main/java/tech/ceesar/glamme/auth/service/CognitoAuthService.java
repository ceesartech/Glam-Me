package tech.ceesar.glamme.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.auth.config.CognitoConfig;
import tech.ceesar.glamme.auth.dto.AuthResult;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.common.service.RedisIdempotencyService;
import tech.ceesar.glamme.common.service.RedisRateLimitService;
import tech.ceesar.glamme.common.service.aws.AppConfigService;
import tech.ceesar.glamme.common.service.aws.CloudWatchMetricsService;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoConfig cognitoConfig;
    private final RedisCacheService cacheService;
    private final RedisRateLimitService rateLimitService;
    private final RedisIdempotencyService idempotencyService;
    private final AppConfigService appConfigService;
    private final CloudWatchMetricsService metricsService;

    public AuthResult loginUser(String username, String password) {
        // Check if authentication is enabled via AppConfig
        if (!appConfigService.getFeatureFlag("auth.login.enabled", true)) {
            throw new RuntimeException("Authentication is currently disabled.");
        }

        // Check rate limiting for authentication attempts
        if (!rateLimitService.checkAuthRateLimit(username)) {
            throw new RuntimeException("Too many authentication attempts. Please try again later.");
        }

        try {
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .clientId(cognitoConfig.getClientId())
                    .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", username,
                            "PASSWORD", password
                    ))
                    .build();

            long startTime = System.currentTimeMillis();
            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);
            long endTime = System.currentTimeMillis();

            if (authResponse.challengeName() != null) {
                throw new RuntimeException("Additional authentication required: " + authResponse.challengeName());
            }

            // Cache successful authentication result briefly
            cacheService.set("auth:token:" + username, authResponse.authenticationResult(), Duration.ofMinutes(5));

            // Publish metrics
            metricsService.recordApiCall("auth.login", "POST", endTime - startTime);
            metricsService.incrementCounter("GlamMe/Auth", "SuccessfulLogins");

            return convertToAuthResult(authResponse.authenticationResult());

        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito authentication failed for user: {}", username, e);
            metricsService.recordError("GlamMe/Auth", "AuthenticationFailed");
            metricsService.incrementCounter("GlamMe/Auth", "FailedLogins");
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public UserType registerUser(String username, String email, String password, String name) {
        try {
            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .userAttributes(
                            AttributeType.builder().name("email").value(email).build(),
                            AttributeType.builder().name("email_verified").value("true").build(),
                            AttributeType.builder().name("name").value(name).build(),
                            AttributeType.builder().name("user_type").value("customer").build(),
                            AttributeType.builder().name("plan").value("FREE").build()
                    )
                    .messageAction(MessageActionType.SUPPRESS)
                    .temporaryPassword(password)
                    .build();

            AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(createUserRequest);

            // Set permanent password
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .password(password)
                    .permanent(true)
                    .build();

            cognitoClient.adminSetUserPassword(setPasswordRequest);

            log.info("User registered successfully: {}", username);
            return createUserResponse.user();

        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito user registration failed for user: {}", username, e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    public void addUserToGroup(String username, String groupName) {
        try {
            AdminAddUserToGroupRequest addToGroupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .groupName(groupName)
                    .build();

            cognitoClient.adminAddUserToGroup(addToGroupRequest);
            log.info("User {} added to group {}", username, groupName);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to add user {} to group {}", username, groupName, e);
            throw new RuntimeException("Failed to add user to group: " + e.getMessage());
        }
    }

    public UserType getUser(String username) {
        // Try to get from cache first
        Optional<UserType> cachedUser = cacheService.get("user:cognito:" + username, UserType.class);
        if (cachedUser.isPresent()) {
            log.debug("Retrieved user {} from cache", username);
            return cachedUser.get();
        }

        try {
            AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .build();

            AdminGetUserResponse getUserResponse = cognitoClient.adminGetUser(getUserRequest);
            UserType user = UserType.builder()
                    .username(username)
                    .attributes(getUserResponse.userAttributes())
                    .userCreateDate(getUserResponse.userCreateDate())
                    .userLastModifiedDate(getUserResponse.userLastModifiedDate())
                    .enabled(getUserResponse.enabled())
                    .userStatus(getUserResponse.userStatus())
                    .build();

            // Cache user data for 30 minutes
            cacheService.set("user:cognito:" + username, user, Duration.ofMinutes(30));

            return user;

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to get user {}", username, e);
            throw new RuntimeException("User not found: " + e.getMessage());
        }
    }

    public void updateUserAttributes(String username, Map<String, String> attributes) {
        try {
            AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .userAttributes(
                            attributes.entrySet().stream()
                                    .map(entry -> AttributeType.builder()
                                            .name(entry.getKey())
                                            .value(entry.getValue())
                                            .build())
                                    .toList()
                    )
                    .build();

            cognitoClient.adminUpdateUserAttributes(updateRequest);
            log.info("User {} attributes updated", username);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to update user {} attributes", username, e);
            throw new RuntimeException("Failed to update user attributes: " + e.getMessage());
        }
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        try {
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .password(newPassword)
                    .permanent(true)
                    .build();

            cognitoClient.adminSetUserPassword(setPasswordRequest);
            log.info("Password changed for user {}", username);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to change password for user {}", username, e);
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }

    public void deleteUser(String username) {
        try {
            AdminDeleteUserRequest deleteUserRequest = AdminDeleteUserRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(username)
                    .build();

            cognitoClient.adminDeleteUser(deleteUserRequest);
            log.info("User {} deleted", username);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to delete user {}", username, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }

    /**
     * Convert AWS Cognito AuthenticationResultType to our AuthResult
     */
    private AuthResult convertToAuthResult(AuthenticationResultType authResult) {
        return AuthResult.builder()
                .accessToken(authResult.accessToken())
                .refreshToken(authResult.refreshToken())
                .idToken(authResult.idToken())
                .tokenType("Bearer")
                .expiresIn(authResult.expiresIn().longValue())
                .issuedAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusSeconds(authResult.expiresIn()))
                .build();
    }
}
