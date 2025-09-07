package tech.ceesar.glamme.shopping.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.shopping.dto.CreateProductRequest;
import tech.ceesar.glamme.shopping.dto.ProductResponse;
import tech.ceesar.glamme.shopping.entity.Product;
import tech.ceesar.glamme.shopping.repositories.ProductRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository repo;
    private final S3Client s3;
    private final RedisCacheService cacheService;
    private final EventPublisher eventPublisher;

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

        // Cache the product
        cacheService.set("product:" + p.getProductId(), p, Duration.ofHours(12));

        // Publish product created event
        eventPublisher.publishEvent("product.created", Map.of(
                "productId", p.getProductId().toString(),
                "name", p.getName(),
                "price", p.getPrice(),
                "sku", p.getSku()
        ));

        log.info("Product created: {} with ID: {}", p.getName(), p.getProductId());

        return new ProductResponse(p.getProductId(),
                p.getName(),p.getDescription(),p.getPrice(),
                p.getWeight(),p.getSku(),p.getImageUrl());
    }

    public List<ProductResponse> list() {
        // Try to get from cache first
        List<ProductResponse> cachedProducts = cacheService.get("products:list", List.class)
                .orElse(null);

        if (cachedProducts != null && !cachedProducts.isEmpty()) {
            log.debug("Retrieved product list from cache");
            return cachedProducts;
        }

        // Get from database
        List<ProductResponse> products = repo.findAll().stream()
                .map(p -> new ProductResponse(
                        p.getProductId(),p.getName(),p.getDescription(),
                        p.getPrice(),p.getWeight(),
                        p.getSku(),p.getImageUrl()))
                .toList();

        // Cache the product list for 1 hour
        cacheService.set("products:list", products, Duration.ofHours(1));

        log.debug("Retrieved {} products from database", products.size());
        return products;
    }

    /**
     * Get product by ID with caching
     */
    public ProductResponse getProduct(UUID productId) {
        // Try cache first
        return cacheService.get("product:" + productId, ProductResponse.class)
                .orElseGet(() -> {
                    // Fallback to database
                    Product product = repo.findById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

                    ProductResponse response = new ProductResponse(
                            product.getProductId(), product.getName(), product.getDescription(),
                            product.getPrice(), product.getWeight(), product.getSku(), product.getImageUrl());

                    // Cache for 6 hours
                    cacheService.set("product:" + productId, response, Duration.ofHours(6));

                    return response;
                });
    }
}
