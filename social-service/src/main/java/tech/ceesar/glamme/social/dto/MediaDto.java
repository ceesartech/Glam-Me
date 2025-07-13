package tech.ceesar.glamme.social.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MediaDto {
    private UUID id;
    private String url;
    private String type;
}
