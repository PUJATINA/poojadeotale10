package com.ecommerse.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryCreateRequest(
        @NotBlank(message = "Category name is required")
        String name
) {
}
