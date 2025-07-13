package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallResponse {
    private String callSid;
    private String status;
}
