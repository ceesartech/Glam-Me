package tech.ceesar.glamme.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.auth.dto.JwtAuthenticationResponse;
import tech.ceesar.glamme.auth.dto.LoginRequest;
import tech.ceesar.glamme.auth.dto.RegisterRequest;
import tech.ceesar.glamme.auth.entity.User;
import tech.ceesar.glamme.auth.repository.UserRepository;
import tech.ceesar.glamme.auth.security.JwtTokenProvider;
import tech.ceesar.glamme.common.enums.Role;
import tech.ceesar.glamme.common.enums.SubscriptionType;
import tech.ceesar.glamme.common.exception.BadRequestException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        var authToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(), loginRequest.getPassword()
        );
        try {
            authManager.authenticate(authToken);
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid credentials");
        }

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials!"));

        String jwt = tokenProvider.generateToken(user.getId());
        return new JwtAuthenticationResponse(jwt);
    }

    public JwtAuthenticationResponse registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email address already in use");
        }
        User toSave = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(Set.of(Role.CUSTOMER))
                .subscriptionType(registerRequest.getSubscriptionType() ==null
                        ? SubscriptionType.FREE
                        : registerRequest.getSubscriptionType()
                )
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(toSave);

        UUID userId = saved.getId();
        String jwt = tokenProvider.generateToken(userId);
        return new JwtAuthenticationResponse(jwt);
    }
}
