package com.ecommerse.backend.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productName,
        String imageUrl,
        String productDescription,
        BigDecimal price,
        Integer quantity
) {
}
