package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsResponse {
    private String messageId;
    private String requestId;
    private Instant timestamp;
    private String status;
    private String providerResponse;
}
