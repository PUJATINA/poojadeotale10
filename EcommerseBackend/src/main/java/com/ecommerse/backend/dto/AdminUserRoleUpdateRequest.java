package com.ecommerse.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserRoleUpdateRequest(
        @NotBlank(message = "Role is required")
        String role
) {
}
