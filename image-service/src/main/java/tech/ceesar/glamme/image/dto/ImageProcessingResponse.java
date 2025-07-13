package tech.ceesar.glamme.image.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response returned after uploading raw image, processing via OpenAI, and uploading result.
 */
@Data
@AllArgsConstructor
public class ImageProcessingResponse {
    private String rawImageUrl;
    private String processedImageUrl;
}
