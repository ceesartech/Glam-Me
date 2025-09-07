package tech.ceesar.glamme.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import tech.ceesar.glamme.common.enums.SubscriptionType;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private SubscriptionType subscriptionType = SubscriptionType.FREE;
}
