package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSmsResponse {
    private List<Success> success;
    private List<Failure> failure;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Success {
        private String messageId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Failure {
        private String error;
    }
}
