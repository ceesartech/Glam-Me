package tech.ceesar.glamme.image.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.ceesar.glamme.image.dto.*;
import tech.ceesar.glamme.image.entity.ImageJob;
import tech.ceesar.glamme.image.service.ImageProcessingService;
import tech.ceesar.glamme.image.service.HairstyleSearchService;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageProcessingController {
    private final ImageProcessingService service;
    private final HairstyleSearchService hairstyleSearchService;

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

    // === HAIRSTYLE SEARCH ENDPOINTS ===

    /**
     * Search hairstyles by text query, category, or filters
     */
    @GetMapping("/hairstyles/search")
    public ResponseEntity<List<HairstyleSearchResponse>> searchHairstyles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer minTime,
            @RequestParam(required = false) Integer maxTime,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        HairstyleSearchRequest request = HairstyleSearchRequest.builder()
                .query(query)
                .category(category)
                .difficulty(difficulty)
                .minTime(minTime)
                .maxTime(maxTime)
                .limit(limit)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        List<HairstyleSearchResponse> results = hairstyleSearchService.searchHairstyles(request);
        return ResponseEntity.ok(results);
    }

    /**
     * Get popular hairstyles by category
     */
    @GetMapping("/hairstyles/popular")
    public ResponseEntity<List<HairstyleSearchResponse>> getPopularHairstyles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<HairstyleSearchResponse> results = hairstyleSearchService.getPopularHairstyles(category, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Get trending hairstyles
     */
    @GetMapping("/hairstyles/trending")
    public ResponseEntity<List<HairstyleSearchResponse>> getTrendingHairstyles(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<HairstyleSearchResponse> results = hairstyleSearchService.getTrendingHairstyles(limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Get hairstyles by categories (for browsing)
     */
    @GetMapping("/hairstyles/categories")
    public ResponseEntity<List<HairstyleSearchResponse>> getHairstylesByCategories(
            @RequestParam List<String> categories,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<HairstyleSearchResponse> results = hairstyleSearchService.getHairstylesByCategories(categories, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Enhanced image processing with hairstyle selection
     */
    @PostMapping("/process-with-hairstyle")
    public ResponseEntity<ImageJobResponse> processImageWithHairstyle(
            @RequestPart("file") MultipartFile file,
            @RequestParam String hairstyleId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "INPAINT") String jobType
    ) {
        // Create an enhanced job request with specific hairstyle template
        ImageJobRequest request = new ImageJobRequest();
        request.setJobType(ImageJob.JobType.valueOf(jobType));
        request.setUserId(userId);
        
        // Get hairstyle template and use its prompt
        // This would need to be implemented in the service
        request.setPrompt("Apply hairstyle with ID: " + hairstyleId + " to the uploaded image");
        
        ImageJobResponse response = service.submitImageJob(request);
        return ResponseEntity.accepted().body(response);
    }
}
