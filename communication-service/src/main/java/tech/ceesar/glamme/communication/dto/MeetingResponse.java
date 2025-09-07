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
public class MeetingResponse {
    private String meetingId;
    private String meetingArn;
    private String externalMeetingId;
    private String mediaRegion;
    private Object mediaPlacement; // Simplified for now
    private Instant createdAt;
}
