package tech.ceesar.glamme.image.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_jobs")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ImageJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "subject_key")
    private String subjectKey;

    @Column(name = "style_ref_key")
    private String styleRefKey;

    @Column(name = "mask_key")
    private String maskKey;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "provider")
    private String provider;

    @Column(name = "output_key")
    private String outputKey;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum JobType {
        INPAINT,
        STYLE_TRANSFER,
        GENERATE
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED
    }
}
