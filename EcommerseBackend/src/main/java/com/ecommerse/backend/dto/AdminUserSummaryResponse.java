package com.ecommerse.backend.dto;

public record AdminUserSummaryResponse(
        Long userId,
        String name,
        String email,
        String role
) {
}
