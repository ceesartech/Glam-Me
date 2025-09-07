package tech.ceesar.glamme.image.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.ceesar.glamme.image.dto.ImageJobRequest;
import tech.ceesar.glamme.image.dto.ImageJobResponse;
import tech.ceesar.glamme.image.dto.ImageProcessingResponse;
import tech.ceesar.glamme.image.service.ImageProcessingService;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageProcessingController {
    private final ImageProcessingService service;

    /**
     * Legacy synchronous endpoint for backwards compatibility
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageProcessingResponse> processImage(
            @RequestPart("file") MultipartFile file,
            @RequestPart("hairStyleType") @NotBlank String hairStyleType
    ) {
        ImageProcessingResponse response = service.processImage(file, hairStyleType);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * New async endpoint for submitting image processing jobs
     */
    @PostMapping("/jobs")
    public ResponseEntity<ImageJobResponse> submitImageJob(@Valid @RequestBody ImageJobRequest request) {
        ImageJobResponse response = service.submitImageJob(request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get job status
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ImageJobResponse> getJobStatus(
            @PathVariable String jobId,
            @RequestParam String userId) {
        ImageJobResponse response = service.getJobStatus(jobId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload file and get presigned URL (for frontend uploads)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        // This would need to be implemented to return a presigned URL for direct upload
        return ResponseEntity.ok("Upload endpoint - to be implemented");
    }
}
