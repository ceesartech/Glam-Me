package tech.ceesar.glamme.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BlockResponse {
    private UUID blockerId;
    private UUID blockedId;
}
