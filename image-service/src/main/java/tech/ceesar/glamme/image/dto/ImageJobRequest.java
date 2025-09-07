package tech.ceesar.glamme.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.ceesar.glamme.image.entity.ImageJob;

@Data
public class ImageJobRequest {

    @NotNull
    private ImageJob.JobType jobType;

    @NotBlank
    private String userId;

    private String subjectKey;

    private String styleRefKey;

    private String maskKey;

    @NotBlank
    private String prompt;

    private String provider; // Optional, will default if not provided

    // For backwards compatibility with the existing API
    public static ImageJobRequest fromLegacyRequest(String hairstyleType, String userId) {
        ImageJobRequest request = new ImageJobRequest();
        request.setJobType(ImageJob.JobType.INPAINT);
        request.setUserId(userId);
        request.setPrompt("Apply the specified hairstyle to this portrait. The hairstyle is " + hairstyleType);
        return request;
    }
}
