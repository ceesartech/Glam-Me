package tech.ceesar.glamme.image.service;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.image.CreateImageEditRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tech.ceesar.glamme.image.dto.ImageProcessingResponse;
import tech.ceesar.glamme.image.exception.ImageProcessingException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageProcessingService {
    private final S3Client s3Client;
    private final OpenAiService openAiService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region}")
    private String region;

    private static final String RAW_PREFIX = "raw/";
    private static final String PROCESSED_PREFIX = "processed/";
    private static final String BASE_PROMPT = "Apply the specified hairstyle to this portrait. The hairstyle is ";

    public ImageProcessingResponse processImage(MultipartFile file, String hairstyleType) {
        try {
            // --- 1) Upload raw image ---
            String rawKey = RAW_PREFIX + UUID.randomUUID() + getExtension(file.getOriginalFilename());
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(rawKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            String rawUrl = buildUrl(rawKey);

            // --- 2) Request OpenAI image-edit ---
            // Write MultipartFile to temp file
            File tmpFile = File.createTempFile("raw-", getExtension(file.getOriginalFilename()));
            file.transferTo(tmpFile);

            CreateImageEditRequest editRequest = CreateImageEditRequest.builder()
                    .prompt(BASE_PROMPT + " " + hairstyleType)
                    .n(1)
                    .size("1024x1024")
                    .build();

            File rawFile = tmpFile;
            File maskFile = null;
            ImageResult editResponse = openAiService.createImageEdit(editRequest, rawFile, maskFile);
            Image editedImage = editResponse.getData().get(0);

            // Download AI edited Image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (InputStream inputStream = new URL(editedImage.getUrl()).openStream()) {
                inputStream.transferTo(outputStream);
            }

            // Clean up temp file
            tmpFile.delete();

            // --- 3) Upload processed image ---
            String processedKey = PROCESSED_PREFIX + UUID.randomUUID() +".png";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(processedKey)
                            .contentType("image/png")
                            .build(),
                    RequestBody.fromBytes(outputStream.toByteArray())
            );

            String processedUrl = buildUrl(processedKey);

            return new ImageProcessingResponse(rawUrl, processedUrl);
        } catch (IOException | RuntimeException ex) {
            throw new ImageProcessingException("Image processing failed", ex);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return (idx >= 0) ? fileName.substring(idx) : "";
    }

    private String buildUrl(String key) {
        String encoded = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, encoded);
    }
}
