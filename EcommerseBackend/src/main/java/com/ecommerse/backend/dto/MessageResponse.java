package com.ecommerse.backend.dto;

public record MessageResponse(
        boolean success,
        String message
) {
}
