package com.ecommerse.backend.dto;

import jakarta.validation.constraints.NotNull;

public record BuyRequest(
        @NotNull(message = "User id is required")
        Long userId
) {
}
