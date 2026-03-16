package com.ecommerse.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryRenameRequest(
        @NotBlank(message = "Current category is required")
        String currentCategory,

        @NotBlank(message = "New category is required")
        String newCategory
) {
}
