package tech.ceesar.glamme.image.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.image.entity.ImageJob;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageJobResponse {

    private String id;
    private String userId;
    private ImageJob.JobType jobType;
    private ImageJob.JobStatus status;
    private String subjectKey;
    private String styleRefKey;
    private String maskKey;
    private String prompt;
    private String provider;
    private String outputKey;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ImageJobResponse fromEntity(ImageJob job) {
        return ImageJobResponse.builder()
                .id(job.getId())
                .userId(job.getUserId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .subjectKey(job.getSubjectKey())
                .styleRefKey(job.getStyleRefKey())
                .maskKey(job.getMaskKey())
                .prompt(job.getPrompt())
                .provider(job.getProvider())
                .outputKey(job.getOutputKey())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
