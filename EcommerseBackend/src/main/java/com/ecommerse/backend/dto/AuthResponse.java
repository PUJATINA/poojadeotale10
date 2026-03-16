package com.ecommerse.backend.dto;

public record AuthResponse(
        boolean success,
        String message,
        Long userId,
        String name,
        String email,
        String role,
        String accessToken,
        String tokenType
) {
}
