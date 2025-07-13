package tech.ceesar.glamme.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Standard error payload for REST APIs.
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final List<String> messages;
    private final String path;
}
