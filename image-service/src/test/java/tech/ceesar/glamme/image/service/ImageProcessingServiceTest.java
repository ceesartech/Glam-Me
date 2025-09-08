package tech.ceesar.glamme.image.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.image.dto.*;
import tech.ceesar.glamme.image.entity.ImageJob;
import tech.ceesar.glamme.image.exception.ImageProcessingException;
import tech.ceesar.glamme.image.repository.ImageJobRepository;

import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ImageJobRepository imageJobRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ImageProcessingService imageProcessingService;

    private MockMultipartFile testFile;
    private ImageJob sampleJob;
    private ImageJobRequest jobRequest;

    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(imageProcessingService, "bucketName", "test-glamme-bucket");
        ReflectionTestUtils.setField(imageProcessingService, "imageJobsQueueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue");

        // Create test file
        testFile = new MockMultipartFile(
                "file", 
                "test-image.jpg", 
                "image/jpeg", 
                "test image content".getBytes()
        );

        // Create sample job
        sampleJob = ImageJob.builder()
                .id("job-123")
                .userId("user-456")
                .jobType(ImageJob.JobType.INPAINT)
                .status(ImageJob.JobStatus.PENDING)
                .subjectKey("uploads/test-subject.jpg")
                .prompt("Apply braids hairstyle to this portrait")
                .provider("amazon.titan-image-generator-v1")
                .build();

        // Create job request
        jobRequest = new ImageJobRequest();
        jobRequest.setJobType(ImageJob.JobType.INPAINT);
        jobRequest.setUserId("user-456");
        jobRequest.setSubjectKey("uploads/test-subject.jpg");
        jobRequest.setPrompt("Apply braids hairstyle to this portrait");
        jobRequest.setProvider("amazon.titan-image-generator-v1");
    }

    @Test
    void processImage_Legacy_Success() throws Exception {
        // Arrange
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        
        when(imageJobRepository.save(any(ImageJob.class))).thenReturn(sampleJob);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/uploads/test.jpg"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // Act
        ImageProcessingResponse result = imageProcessingService.processImage(testFile, "braids");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRawImageUrl());
        assertTrue(result.getRawImageUrl().contains("test-bucket"));
        assertNotNull(result.getProcessedImageUrl());
        assertTrue(result.getProcessedImageUrl().contains("Job submitted"));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(imageJobRepository).save(any(ImageJob.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void submitImageJob_Success() {
        // Arrange
        when(imageJobRepository.save(any(ImageJob.class))).thenReturn(sampleJob);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        // Act
        ImageJobResponse result = imageProcessingService.submitImageJob(jobRequest);

        // Assert
        assertNotNull(result);
        assertEquals(sampleJob.getId(), result.getId());
        assertEquals(sampleJob.getUserId(), result.getUserId());
        assertEquals(sampleJob.getJobType(), result.getJobType());
        assertEquals(sampleJob.getStatus(), result.getStatus());

        verify(imageJobRepository).save(any(ImageJob.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void submitImageJob_WithStyleTransfer_Success() {
        // Arrange
        jobRequest.setJobType(ImageJob.JobType.STYLE_TRANSFER);
        jobRequest.setStyleRefKey("styles/reference-style.jpg");

        ImageJob styleTransferJob = sampleJob.toBuilder()
                .jobType(ImageJob.JobType.STYLE_TRANSFER)
                .styleRefKey("styles/reference-style.jpg")
                .build();

        when(imageJobRepository.save(any(ImageJob.class))).thenReturn(styleTransferJob);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        // Act
        ImageJobResponse result = imageProcessingService.submitImageJob(jobRequest);

        // Assert
        assertNotNull(result);
        assertEquals(ImageJob.JobType.STYLE_TRANSFER, result.getJobType());
        assertEquals("styles/reference-style.jpg", result.getStyleRefKey());

        verify(imageJobRepository).save(any(ImageJob.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void submitImageJob_WithTextGeneration_Success() {
        // Arrange
        jobRequest.setJobType(ImageJob.JobType.GENERATE);
        jobRequest.setSubjectKey(null); // No subject image for text generation
        jobRequest.setPrompt("Generate a portrait with curly hair and professional styling");

        ImageJob generateJob = sampleJob.toBuilder()
                .jobType(ImageJob.JobType.GENERATE)
                .subjectKey(null)
                .prompt("Generate a portrait with curly hair and professional styling")
                .build();

        when(imageJobRepository.save(any(ImageJob.class))).thenReturn(generateJob);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        // Act
        ImageJobResponse result = imageProcessingService.submitImageJob(jobRequest);

        // Assert
        assertNotNull(result);
        assertEquals(ImageJob.JobType.GENERATE, result.getJobType());
        assertNull(result.getSubjectKey());
        assertEquals("Generate a portrait with curly hair and professional styling", result.getPrompt());

        verify(imageJobRepository).save(any(ImageJob.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void submitImageJob_SQSFailure_ThrowsException() {
        // Arrange
        when(imageJobRepository.save(any(ImageJob.class))).thenReturn(sampleJob);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS connection failed"));

        // Act & Assert
        assertThrows(ImageProcessingException.class, () -> 
            imageProcessingService.submitImageJob(jobRequest));

        verify(imageJobRepository).save(any(ImageJob.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void getJobStatus_Success() {
        // Arrange
        String jobId = "job-123";
        String userId = "user-456";

        when(imageJobRepository.findByIdAndUserId(jobId, userId))
                .thenReturn(Optional.of(sampleJob));

        // Act
        ImageJobResponse result = imageProcessingService.getJobStatus(jobId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(sampleJob.getId(), result.getId());
        assertEquals(sampleJob.getUserId(), result.getUserId());
        assertEquals(sampleJob.getStatus(), result.getStatus());

        verify(imageJobRepository).findByIdAndUserId(jobId, userId);
    }

    @Test
    void getJobStatus_JobNotFound_ThrowsException() {
        // Arrange
        String jobId = "non-existent";
        String userId = "user-456";

        when(imageJobRepository.findByIdAndUserId(jobId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ImageProcessingException.class, () -> 
            imageProcessingService.getJobStatus(jobId, userId));

        verify(imageJobRepository).findByIdAndUserId(jobId, userId);
    }

    @Test
    void processImage_S3UploadFailure_ThrowsException() throws Exception {
        // Arrange
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // Act & Assert
        assertThrows(ImageProcessingException.class, () -> 
            imageProcessingService.processImage(testFile, "braids"));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(imageJobRepository, never()).save(any());
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void submitImageJob_DatabaseSaveFailure_ThrowsException() {
        // Arrange
        when(imageJobRepository.save(any(ImageJob.class)))
                .thenThrow(new RuntimeException("Database save failed"));

        // Act & Assert
        assertThrows(ImageProcessingException.class, () -> 
            imageProcessingService.submitImageJob(jobRequest));

        verify(imageJobRepository).save(any(ImageJob.class));
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void submitImageJob_DefaultProvider_UsesCorrectProvider() {
        // Arrange
        jobRequest.setProvider(null); // Test default provider assignment

        when(imageJobRepository.save(any(ImageJob.class))).thenReturn(sampleJob);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        // Act
        ImageJobResponse result = imageProcessingService.submitImageJob(jobRequest);

        // Assert
        assertNotNull(result);
        
        // Verify the saved job used the default provider
        verify(imageJobRepository).save(argThat(job -> 
            "amazon.titan-image-generator-v1".equals(job.getProvider())));
    }

    @Test
    void getJobStatus_WrongUser_ThrowsException() {
        // Arrange
        String jobId = "job-123";
        String wrongUserId = "wrong-user";

        when(imageJobRepository.findByIdAndUserId(jobId, wrongUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ImageProcessingException.class, () -> 
            imageProcessingService.getJobStatus(jobId, wrongUserId));

        verify(imageJobRepository).findByIdAndUserId(jobId, wrongUserId);
    }
}