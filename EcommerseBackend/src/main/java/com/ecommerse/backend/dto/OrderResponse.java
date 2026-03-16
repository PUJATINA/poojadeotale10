package com.ecommerse.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long userId,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        String message
) {
}
