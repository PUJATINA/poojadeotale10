package com.ecommerse.backend.dto;

import com.ecommerse.backend.entity.Product;

import java.util.List;

public record ProductPageResponse(
        List<Product> items,
        int currentPage,
        int pageSize,
        int totalPages,
        long totalItems
) {
}
