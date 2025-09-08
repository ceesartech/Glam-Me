package tech.ceesar.glamme.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.image.dto.ImageJobRequest;
import tech.ceesar.glamme.image.dto.ImageJobResponse;
import tech.ceesar.glamme.image.dto.ImageProcessingResponse;
import tech.ceesar.glamme.image.entity.ImageJob;
import tech.ceesar.glamme.image.exception.ImageProcessingException;
import tech.ceesar.glamme.image.repository.ImageJobRepository;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final SqsClient sqsClient;
    private final ImageJobRepository imageJobRepository;
    private final EventPublisher eventPublisher;

    @Value("${aws.s3.bucket:glamme-media}")
    private String bucketName;

    @Value("${aws.sqs.image-jobs-queue-url}")
    private String imageJobsQueueUrl;

    private static final String UPLOADS_PREFIX = "uploads/";
    private static final String OUTPUTS_PREFIX = "outputs/";

    /**
     * Legacy synchronous method for backwards compatibility
     */
    public ImageProcessingResponse processImage(MultipartFile file, String hairstyleType) {
        try {
            // Upload raw image
            String subjectKey = uploadFile(file, UPLOADS_PREFIX);

            // Create job request
            ImageJobRequest request = ImageJobRequest.fromLegacyRequest(hairstyleType, "anonymous");
            request.setSubjectKey(subjectKey);

            // Submit async job
            ImageJobResponse jobResponse = submitImageJob(request);

            // For legacy compatibility, we could wait for completion, but for now return job info
            return new ImageProcessingResponse(
                buildPresignedUrl(subjectKey),
                "Job submitted: " + jobResponse.getId()
            );

        } catch (IOException ex) {
            throw new ImageProcessingException("Image processing failed", ex);
        } catch (Exception ex) {
            throw new ImageProcessingException("Image processing failed", ex);
        }
    }

    /**
     * New async method for submitting image processing jobs
     */
    public ImageJobResponse submitImageJob(ImageJobRequest request) {
        try {
            // Create and save job entity
            ImageJob job = ImageJob.builder()
                    .userId(request.getUserId())
                    .jobType(request.getJobType())
                    .status(ImageJob.JobStatus.PENDING)
                    .subjectKey(request.getSubjectKey())
                    .styleRefKey(request.getStyleRefKey())
                    .maskKey(request.getMaskKey())
                    .prompt(request.getPrompt())
                    .provider(request.getProvider() != null ? request.getProvider() : "amazon.titan-image-generator-v1")
                    .build();

            ImageJob savedJob = imageJobRepository.save(job);

            // Send message to SQS
            SendMessageRequest sqsRequest = SendMessageRequest.builder()
                    .queueUrl(imageJobsQueueUrl)
                    .messageBody(savedJob.getId())
                    .build();

            sqsClient.sendMessage(sqsRequest);

            log.info("Submitted image job: {}", savedJob.getId());

            return ImageJobResponse.fromEntity(savedJob);

        } catch (Exception e) {
            log.error("Failed to submit image job", e);
            throw new ImageProcessingException("Failed to submit image job", e);
        }
    }

    /**
     * Get job status by ID
     */
    public ImageJobResponse getJobStatus(String jobId, String userId) {
        ImageJob job = imageJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ImageProcessingException("Job not found"));

        return ImageJobResponse.fromEntity(job);
    }

    private String uploadFile(MultipartFile file, String prefix) throws IOException {
        String key = prefix + UUID.randomUUID() + getExtension(file.getOriginalFilename());

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return key;
    }

    private String buildPresignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(b -> b.bucket(bucketName).key(key))
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return (idx >= 0) ? fileName.substring(idx) : "";
    }
}
