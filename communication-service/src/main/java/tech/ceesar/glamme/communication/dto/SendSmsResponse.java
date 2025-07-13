package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendSmsResponse {
    private String messageSid;
    private String status;
}
