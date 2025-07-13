package tech.ceesar.glamme.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.ceesar.glamme.auth.dto.JwtAuthenticationResponse;
import tech.ceesar.glamme.auth.dto.LoginRequest;
import tech.ceesar.glamme.auth.dto.RegisterRequest;
import tech.ceesar.glamme.auth.entity.User;
import tech.ceesar.glamme.auth.repository.UserRepository;
import tech.ceesar.glamme.auth.security.JwtTokenProvider;
import tech.ceesar.glamme.common.enums.SubscriptionType;
import tech.ceesar.glamme.common.exception.BadRequestException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    @Mock AuthenticationManager authManager;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;
    @InjectMocks AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_success() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("CJ");
        registerRequest.setEmail("cj@testglamme.com");
        registerRequest.setPassword("MyPassWord");
        registerRequest.setSubscriptionType(SubscriptionType.PREMIUM);

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("MyPassWord")).thenReturn("MyEncodedPassWord");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(tokenProvider.generateToken(any(UUID.class))).thenReturn("token");

        JwtAuthenticationResponse authResponse = authService.registerUser(registerRequest);
        assertEquals("token", authResponse.getAccessToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_duplicateEmail() {
        when(userRepository.existsByEmail("sarah@testglamme.com")).thenReturn(true);
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Sarah");
        registerRequest.setEmail("sarah@testglamme.com");
        registerRequest.setPassword("TestPassword");
        assertThrows(BadRequestException.class, () -> authService.registerUser(registerRequest));
    }

    @Test
    void authenticateUser_invalidRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("somto@testglamme.com");
        loginRequest.setPassword("WrongPassword");
        doThrow(BadCredentialsException.class)
                .when(authManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadRequestException.class, () -> authService.authenticateUser(loginRequest));
    }

    @Test
    void authenticateUser_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("steph@testglamme.com");
        loginRequest.setPassword("DummyPassword");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("steph@testglamme.com");

        when(authManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("steph@testglamme.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(user.getId())).thenReturn("jwt");

        JwtAuthenticationResponse authResponse = authService.authenticateUser(loginRequest);
        assertEquals("jwt", authResponse.getAccessToken());
    }
}
