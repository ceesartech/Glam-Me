package tech.ceesar.glamme.image.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.ceesar.glamme.image.dto.ImageProcessingResponse;
import tech.ceesar.glamme.image.service.ImageProcessingService;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageProcessingController {
    private final ImageProcessingService service;

    /**
     * Uploads an image + hairstyleType, returns URLs for raw & AI‑edited images.
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageProcessingResponse processImage(
            @RequestPart("file")MultipartFile file,
            @RequestPart("hairStyleType") @NotBlank String hairStyleType
    ) {
        return service.processImage(file, hairStyleType);
    }
}
