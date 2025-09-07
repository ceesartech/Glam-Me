package tech.ceesar.glamme.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String username;
    private String email;
    private String name;
    private String userType;
    private String plan;
    private Boolean enabled;
    private String status;
}
