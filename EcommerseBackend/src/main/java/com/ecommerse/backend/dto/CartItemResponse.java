package com.ecommerse.backend.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal price,
        Integer quantity,
        BigDecimal lineTotal
) {
}
