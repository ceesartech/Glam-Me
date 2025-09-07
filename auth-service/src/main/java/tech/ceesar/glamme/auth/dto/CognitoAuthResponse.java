package tech.ceesar.glamme.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CognitoAuthResponse {
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String tokenType;
    private Integer expiresIn;
    private String message;
    private String userSub;
}
