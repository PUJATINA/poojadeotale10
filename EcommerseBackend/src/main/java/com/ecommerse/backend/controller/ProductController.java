package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.ProductPageResponse;
import com.ecommerse.backend.entity.Product;
import com.ecommerse.backend.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse listProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API hit: list products category={} brand={} query={} page={} size={}", category, brand, query, page, size);
        return productService.getProducts(category, brand, query, sortBy, sortDir, page, size);
    }

    @GetMapping("/{productId}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long productId) {
        log.info("API hit: get product image productId={}", productId);
        Product product = productService.getProductById(productId);

        byte[] imageBytes = product.getImageData();
        String contentType = product.getImageContentType();
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("Product image missing for productId={}, using fallback image", productId);
            imageBytes = loadDefaultImage();
            contentType = "image/svg+xml";
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            mediaType = MediaType.parseMediaType(contentType);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(imageBytes);
    }

    private byte[] loadDefaultImage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/product-placeholder.svg");
            return resource.getInputStream().readAllBytes();
        } catch (IOException exception) {
            log.error("Failed to load fallback product image", exception);
            throw new UncheckedIOException("Unable to load fallback product image", exception);
        }
    }
}
