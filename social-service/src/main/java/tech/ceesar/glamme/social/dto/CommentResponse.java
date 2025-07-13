package tech.ceesar.glamme.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CommentResponse {
    private UUID id;
    private UUID userId;
    private String content;
    private Instant createdAt;
}
