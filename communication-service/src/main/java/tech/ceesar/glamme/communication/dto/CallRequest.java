package tech.ceesar.glamme.communication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CallRequest {
    @NotBlank private String fromNumber;
    @NotBlank private String toNumber;
}
