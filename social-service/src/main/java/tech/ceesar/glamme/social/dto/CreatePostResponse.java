package tech.ceesar.glamme.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreatePostResponse {
    private UUID postId;
}
