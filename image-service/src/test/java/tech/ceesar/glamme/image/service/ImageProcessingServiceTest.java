package tech.ceesar.glamme.image.service;

import com.theokanning.openai.image.CreateImageEditRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import tech.ceesar.glamme.image.dto.ImageProcessingResponse;
import tech.ceesar.glamme.image.exception.ImageProcessingException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageProcessingServiceTest {

    @Mock S3Client mockS3Client;
    @Mock OpenAiService openAiService;
    @InjectMocks ImageProcessingService imageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // stub @Value‑injected fields
        ReflectionTestUtils.setField(imageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(imageService, "region",     "us-east-1");
    }

    @Test
    void processImage_success() throws Exception {
        // 1) prepare the incoming multipart file
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "dummy-data".getBytes()
        );

        // 2) stub S3 uploads for raw + processed
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // 3) create a small temp file to stand in for the AI-edited image
        File aiOut = File.createTempFile("ai-out-", ".png");
        try (FileOutputStream fos = new FileOutputStream(aiOut)) {
            fos.write("fake-image-bytes".getBytes());
        }
        // ensure cleanup
        aiOut.deleteOnExit();

        // 4) stub the OpenAI call, returning an ImageResult whose URL is our local file
        Image aiImage = new Image();
        aiImage.setUrl(aiOut.toURI().toString());     // file:///tmp/ai-out-xxxx.png
        ImageResult aiResult = new ImageResult();     // no-arg constructor
        aiResult.setData(List.of(aiImage));

        when(openAiService.createImageEdit(
                any(CreateImageEditRequest.class),
                any(File.class),
                isNull(File.class)
        )).thenReturn(aiResult);

        // 5) run the service
        ImageProcessingResponse resp = imageService.processImage(file, "boho braids");

        // 6) assertions
        assertNotNull(resp.getRawImageUrl());
        assertTrue(resp.getRawImageUrl().contains("test-bucket"));
        assertNotNull(resp.getProcessedImageUrl());
        assertTrue(resp.getProcessedImageUrl().contains("test-bucket"));

        // 7) verify interaction
        verify(openAiService, times(1)).createImageEdit(
                any(CreateImageEditRequest.class),
                any(File.class),
                isNull(File.class)
        );
    }

    @Test
    void processImage_s3Failure_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.jpg", "image/jpeg", new byte[0]
        );
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(RuntimeException.class);

        assertThrows(ImageProcessingException.class,
                () -> imageService.processImage(file, "fade"));
    }
}
