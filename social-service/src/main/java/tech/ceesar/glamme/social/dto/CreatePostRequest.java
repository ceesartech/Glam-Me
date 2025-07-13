package tech.ceesar.glamme.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreatePostRequest {
    @NotBlank
    private String caption;

    // stylist userIds to tag
    private List<UUID> stylistIds = List.of();
}
