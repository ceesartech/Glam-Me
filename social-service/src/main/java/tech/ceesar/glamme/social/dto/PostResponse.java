package tech.ceesar.glamme.social.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PostResponse {
    private UUID id;
    private UUID userId;
    private String caption;
    private Instant createdAt;
    private List<MediaDto> media;
    private List<UUID> tags;
    private long likeCount;
    private long commentCount;
    private long repostCount;
    private UUID originalPostId; // if repost
}
