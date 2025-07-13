package tech.ceesar.glamme.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateProductRequest {
    @NotBlank
    private String name;

    private String description;

    @Positive
    private double price;

    @Positive
    private double weight;

    @NotBlank
    private String sku;
    // image file

    @NotNull
    private MultipartFile image;
}
