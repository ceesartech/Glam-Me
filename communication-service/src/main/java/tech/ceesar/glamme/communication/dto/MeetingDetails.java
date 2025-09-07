package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDetails {
    private String meetingId;
    private String meetingArn;
    private String externalMeetingId;
    private String mediaRegion;
    private String meetingHostId;
    private Instant createdAt;
    private List<AttendeeInfo> attendees;
}
