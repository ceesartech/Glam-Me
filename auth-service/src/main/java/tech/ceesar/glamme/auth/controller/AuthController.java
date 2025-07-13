package tech.ceesar.glamme.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.auth.dto.JwtAuthenticationResponse;
import tech.ceesar.glamme.auth.dto.LoginRequest;
import tech.ceesar.glamme.auth.dto.RegisterRequest;
import tech.ceesar.glamme.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
            ) {
        JwtAuthenticationResponse authenticationResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(authenticationResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<JwtAuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        JwtAuthenticationResponse authenticationResponse = authService.registerUser(registerRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authenticationResponse);
    }

    // OAuth2 callback endpoints are handled by Spring Security +
    // OAuth2AuthenticationSuccessHandler; no controller needed here.
}
