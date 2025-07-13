package tech.ceesar.glamme.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class JwtAuthenticationResponse {
    private final String accessToken;
    private String tokenType = "Bearer";
}
