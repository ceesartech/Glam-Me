package tech.ceesar.glamme.communication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendSmsRequest {
    @NotBlank private String fromNumber;
    @NotBlank private String toNumber;
    @NotBlank private String message;
}
