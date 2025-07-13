package tech.ceesar.glamme.shopping.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tech.ceesar.glamme.shopping.dto.CreateProductRequest;
import tech.ceesar.glamme.shopping.dto.ProductResponse;
import tech.ceesar.glamme.shopping.entity.Product;
import tech.ceesar.glamme.shopping.repositories.ProductRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public ProductResponse create(CreateProductRequest req) {
        // upload image
        String ext = Optional.ofNullable(req.getImage().getOriginalFilename())
                .filter(n->n.contains("."))
                .map(n->n.substring(n.lastIndexOf('.'))).orElse("");
        String key = "products/" + UUID.randomUUID() + ext;
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(key)
                            .contentType(req.getImage().getContentType()).build(),
                    RequestBody.fromInputStream(
                            req.getImage().getInputStream(),
                            req.getImage().getSize()
                    ));
        } catch (IOException e) {
            throw new RuntimeException("Image upload failed", e);
        }
        String url = "https://" + bucket + ".s3.amazonaws.com/" +
                URLEncoder.encode(key, StandardCharsets.UTF_8);

        Product p = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .weight(req.getWeight())
                .sku(req.getSku())
                .sellerId(null)
                .imageUrl(url)
                .build();
        p = repo.save(p);
        return new ProductResponse(p.getProductId(),
                p.getName(),p.getDescription(),p.getPrice(),
                p.getWeight(),p.getSku(),p.getImageUrl());
    }

    public List<ProductResponse> list() {
        return repo.findAll().stream()
                .map(p -> new ProductResponse(
                        p.getProductId(),p.getName(),p.getDescription(),
                        p.getPrice(),p.getWeight(),
                        p.getSku(),p.getImageUrl()))
                .toList();
    }
}
