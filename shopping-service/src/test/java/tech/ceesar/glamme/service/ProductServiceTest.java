package tech.ceesar.glamme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import tech.ceesar.glamme.shopping.dto.CreateProductRequest;
import tech.ceesar.glamme.shopping.dto.ProductResponse;
import tech.ceesar.glamme.shopping.entity.Product;
import tech.ceesar.glamme.shopping.repositories.ProductRepository;
import tech.ceesar.glamme.shopping.service.ProductService;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.common.event.EventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ProductServiceTest {
    @Mock ProductRepository repo;
    @Mock S3Client s3;
    @Mock RedisCacheService cacheService;
    @Mock EventPublisher eventPublisher;
    @InjectMocks ProductService productService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(productService, "bucket", "test-bucket");
    }

    @Test
    void create_uploadsAndSaves() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "image","img.png","image/png","data".getBytes());
        CreateProductRequest req = new CreateProductRequest();
        req.setName("Test Product"); 
        req.setDescription("Test Description");
        req.setPrice(25.99); 
        req.setWeight(100);
        req.setSku("SKU123"); 
        req.setImage(img);

        // Mock saved product with ID
        Product savedProduct = Product.builder()
                .productId(java.util.UUID.randomUUID())
                .name("Test Product")
                .description("Test Description")
                .price(25.99)
                .weight(100)
                .sku("SKU123")
                .imageUrl("https://test-bucket.s3.amazonaws.com/products/test-image.png")
                .build();

        when(repo.save(any(Product.class))).thenReturn(savedProduct);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ProductResponse r = productService.create(req);
        assertEquals("Test Product", r.getName());
        assertEquals("Test Description", r.getDescription());
        assertEquals(25.99, r.getPrice());
        assertTrue(r.getImageUrl().contains("test-bucket"));
    }
}
