package tech.ceesar.glamme.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.auth.dto.*;
import tech.ceesar.glamme.auth.service.CognitoAuthService;
import tech.ceesar.glamme.auth.service.UserSyncService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CognitoAuthController {

    private final CognitoAuthService cognitoAuthService;
    private final UserSyncService userSyncService;

    @PostMapping("/login")
    public ResponseEntity<CognitoAuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        var authResult = cognitoAuthService.loginUser(loginRequest.getUsername(), loginRequest.getPassword());

        CognitoAuthResponse response = CognitoAuthResponse.builder()
                .accessToken(authResult.getAccessToken())
                .refreshToken(authResult.getRefreshToken())
                .idToken(authResult.getIdToken())
                .tokenType("Bearer")
                .expiresIn(authResult.getExpiresIn().intValue())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<CognitoAuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        var user = cognitoAuthService.registerUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getName()
        );

        // For registration, we typically don't return tokens immediately
        // User needs to verify email or login separately
        CognitoAuthResponse response = CognitoAuthResponse.builder()
                .message("User registered successfully. Please login to continue.")
                .userSub(user.username())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<CognitoAuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshRequest) {
        // This would typically involve calling Cognito's refresh token endpoint
        // For now, we'll implement a basic version
        CognitoAuthResponse response = CognitoAuthResponse.builder()
                .message("Token refresh not yet implemented")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String cognitoSub = jwt.getSubject();
        String username = jwt.getClaimAsString("cognito:username");

        // Sync user data and get from local database
        var localUser = userSyncService.getOrCreateUser(cognitoSub, username);

        UserProfileResponse response = UserProfileResponse.builder()
                .username(localUser.getUsername())
                .email(localUser.getEmail())
                .name(localUser.getName())
                .userType(localUser.getUserType())
                .plan(localUser.getPlan())
                .enabled(localUser.getEnabled())
                .status("ACTIVE") // Cognito users are active if they can authenticate
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        String cognitoSub = jwt.getSubject();
        userSyncService.updateUserProfile(cognitoSub, updateRequest.getAttributes());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody ChangePasswordRequest changeRequest) {
        String username = jwt.getClaimAsString("cognito:username");
        cognitoAuthService.changePassword(username, changeRequest.getOldPassword(), changeRequest.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("cognito:username");
        cognitoAuthService.deleteUser(username);
        return ResponseEntity.ok().build();
    }
}
