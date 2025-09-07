package tech.ceesar.glamme.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
// import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient; // Commented out - not available in this SDK version
// import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
// import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.image.entity.ImageJob;
import tech.ceesar.glamme.image.repository.ImageJobRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageJobWorker {

    private final SqsClient sqsClient;
    private final S3Client s3Client;
    // private final BedrockRuntimeClient bedrockClient;
    private final ImageJobRepository imageJobRepository;
    private final EventPublisher eventPublisher;

    @Value("${aws.s3.bucket:glamme-media}")
    private String bucketName;

    @Value("${aws.sqs.image-jobs-queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void processImageJobs() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(5)
                    .visibilityTimeout(300)
                    .build();

            List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

            for (Message message : messages) {
                processMessage(message);
            }

        } catch (Exception e) {
            log.error("Error processing image jobs", e);
        }
    }

    private void processMessage(Message message) {
        String jobId = message.body();

        try {
            ImageJob job = imageJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            // Update status to RUNNING
            job.setStatus(ImageJob.JobStatus.RUNNING);
            imageJobRepository.save(job);

            // Process the job based on type
            String outputKey = processJob(job);

            // Update job as successful
            job.setStatus(ImageJob.JobStatus.SUCCEEDED);
            job.setOutputKey(outputKey);
            imageJobRepository.save(job);

            // Publish success event
            eventPublisher.publishEvent("image.job.completed",
                    Map.of(
                            "jobId", jobId,
                            "userId", job.getUserId(),
                            "outputKey", outputKey,
                            "jobType", job.getJobType().toString(),
                            "provider", job.getProvider()
                    ));

            // Delete message from queue
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);

            log.info("Successfully processed image job: {}", jobId);

        } catch (Exception e) {
            log.error("Failed to process image job: {}", jobId, e);

            try {
                ImageJob job = imageJobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    job.setStatus(ImageJob.JobStatus.FAILED);
                    job.setErrorMessage(e.getMessage());
                    imageJobRepository.save(job);

                    // Publish failure event
                    eventPublisher.publishEvent("image.job.failed", Map.of(
                            "jobId", jobId,
                            "userId", job.getUserId(),
                            "error", e.getMessage(),
                            "jobType", job.getJobType().toString(),
                            "provider", job.getProvider()
                    ));
                }
            } catch (Exception ex) {
                log.error("Failed to update job status", ex);
            }
        }
    }

    private String processJob(ImageJob job) throws Exception {
        switch (job.getJobType()) {
            case INPAINT:
                return processInpaintJob(job);
            case STYLE_TRANSFER:
                return processStyleTransferJob(job);
            case GENERATE:
                return processGenerateJob(job);
            default:
                throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
        }
    }

    private String processInpaintJob(ImageJob job) throws Exception {
        // Get subject image from S3
        byte[] subjectImage = getImageFromS3(job.getSubjectKey());

        // Prepare Bedrock request for Titan Image Generator
        String requestBody = String.format("""
                {
                    "taskType": "INPAINTING",
                    "inPaintingParams": {
                        "image": "%s",
                        "maskPrompt": "hair",
                        "text": "%s"
                    },
                    "imageGenerationConfig": {
                        "numberOfImages": 1,
                        "quality": "standard",
                        "cfgScale": 8.0,
                        "seed": 42
                    }
                }
                """, Base64.getEncoder().encodeToString(subjectImage), job.getPrompt());

        // TODO: Replace with OpenAI or other AI service implementation
        // For now, return a placeholder image URL
        log.info("AI image generation would be implemented here using OpenAI or similar service");
        return saveImageOutput(job, "placeholder-image-url-" + job.getId());
    }

    private String processStyleTransferJob(ImageJob job) throws Exception {
        // Get images from S3
        byte[] subjectImage = getImageFromS3(job.getSubjectKey());
        byte[] styleImage = getImageFromS3(job.getStyleRefKey());

        // Prepare Bedrock request
        String requestBody = String.format("""
                {
                    "taskType": "IMAGE_VARIATION",
                    "imageVariationParams": {
                        "images": ["%s", "%s"],
                        "text": "%s"
                    },
                    "imageGenerationConfig": {
                        "numberOfImages": 1,
                        "quality": "standard"
                    }
                }
                """, Base64.getEncoder().encodeToString(subjectImage),
                   Base64.getEncoder().encodeToString(styleImage), job.getPrompt());

        // TODO: Replace with OpenAI or other AI service implementation
        // For now, return a placeholder image URL
        log.info("AI style transfer would be implemented here using OpenAI or similar service");
        return saveImageOutput(job, "placeholder-style-transfer-url-" + job.getId());
    }

    private String processGenerateJob(ImageJob job) throws Exception {
        // Prepare Bedrock request for text-to-image
        String requestBody = String.format("""
                {
                    "taskType": "TEXT_IMAGE",
                    "textToImageParams": {
                        "text": "%s"
                    },
                    "imageGenerationConfig": {
                        "numberOfImages": 1,
                        "quality": "standard",
                        "cfgScale": 8.0,
                        "seed": 42
                    }
                }
                """, job.getPrompt());

        // TODO: Replace with OpenAI or other AI service implementation
        // For now, return a placeholder image URL
        log.info("AI text-to-image generation would be implemented here using OpenAI or similar service");
        return saveImageOutput(job, "placeholder-text-to-image-url-" + job.getId());
    }

    private byte[] getImageFromS3(String key) throws Exception {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (InputStream inputStream = s3Client.getObject(request);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String saveImageOutput(ImageJob job, String bedrockResponse) throws Exception {
        // Parse Bedrock response and extract image data
        // This is a simplified version - actual implementation would parse the JSON response
        String outputKey = "outputs/" + job.getId() + ".png";

        // In a real implementation, you'd extract the base64 image from the response
        // and decode it to bytes before saving to S3
        byte[] imageBytes = Base64.getDecoder().decode("fake-image-data"); // Placeholder

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(outputKey)
                .contentType("image/png")
                .build();

        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromBytes(imageBytes));

        return outputKey;
    }
}
