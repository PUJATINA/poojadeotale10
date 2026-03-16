package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.ProductPageResponse;
import com.ecommerse.backend.entity.Product;
import com.ecommerse.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductPageResponse getProducts(
            String category,
            String brand,
            String query,
            String sortBy,
            String sortDir,
            int page,
            int size) {
        log.info("Fetching products category={} brand={} query={} sortBy={} sortDir={} page={} size={}",
                category, brand, query, sortBy, sortDir, page, size);
        Comparator<Product> comparator = buildComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        String normalizedCategory = normalize(category);
        String normalizedBrand = normalize(brand);
        String normalizedQuery = normalize(query);

        List<Product> filtered = productRepository.findAll().stream()
                .filter(product -> normalizedCategory == null ||
                        (product.getCategory() != null && product.getCategory().equalsIgnoreCase(normalizedCategory)))
                .filter(product -> normalizedBrand == null ||
                        (product.getBrand() != null && product.getBrand().equalsIgnoreCase(normalizedBrand)))
                .filter(product -> normalizedQuery == null ||
                        (product.getName() != null &&
                                product.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery.toLowerCase(Locale.ROOT))))
                .sorted(comparator)
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        int totalItems = filtered.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / safeSize);
        if (safePage >= totalPages) {
            safePage = totalPages - 1;
        }

        int fromIndex = safePage * safeSize;
        int toIndex = Math.min(fromIndex + safeSize, totalItems);
        List<Product> pageItems = fromIndex >= toIndex ? List.of() : filtered.subList(fromIndex, toIndex);

        ProductPageResponse response = new ProductPageResponse(pageItems, safePage, safeSize, totalPages, totalItems);
        log.info("Products fetched count={} currentPage={} totalPages={} totalItems={}",
                pageItems.size(), safePage, totalPages, totalItems);
        return response;
    }

    public Product getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        log.info("Product fetched by id={} name={}", productId, product.getName());
        return product;
    }

    private Comparator<Product> buildComparator(String sortBy) {
        if ("price".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Product::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("stock".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Product::getStock, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("category".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(product ->
                            product.getCategory() == null ? "" : product.getCategory().toLowerCase(Locale.ROOT),
                    Comparator.naturalOrder());
        }
        if ("brand".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(product ->
                            product.getBrand() == null ? "" : product.getBrand().toLowerCase(Locale.ROOT),
                    Comparator.naturalOrder());
        }
        return Comparator.comparing(product ->
                        Objects.requireNonNullElse(product.getName(), "").toLowerCase(Locale.ROOT),
                Comparator.naturalOrder());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
