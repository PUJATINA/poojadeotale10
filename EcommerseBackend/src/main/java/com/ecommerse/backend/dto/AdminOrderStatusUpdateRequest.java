package com.ecommerse.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderStatusUpdateRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
