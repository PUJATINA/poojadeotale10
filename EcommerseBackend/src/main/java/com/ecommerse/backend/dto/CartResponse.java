package com.ecommerse.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
}
