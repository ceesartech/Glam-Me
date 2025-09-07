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
public class ChannelResponse {
    private String channelArn;
    private String name;
    private String mode;
    private String privacy;
    private Instant createdTimestamp;
    private Instant lastMessageTimestamp;
}
