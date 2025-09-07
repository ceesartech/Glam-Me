package tech.ceesar.glamme.auth.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateProfileRequest {
    private Map<String, String> attributes;
}
