package tech.ceesar.glamme.shopping.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.shopping.dto.CreateProductRequest;
import tech.ceesar.glamme.shopping.dto.ProductResponse;
import tech.ceesar.glamme.shopping.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/shopping/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> create(
            @RequestPart("data") CreateProductRequest req) {
        return ResponseEntity.status(201).body(productService.create(req));
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.list();
    }
}
