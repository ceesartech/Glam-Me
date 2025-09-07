package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {
    private String messageId;
    private String requestId;
    private Instant timestamp;
    private List<String> successfulRecipients;
    private List<String> failedRecipients;
}
