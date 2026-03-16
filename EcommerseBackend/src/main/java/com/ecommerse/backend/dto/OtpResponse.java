package com.ecommerse.backend.dto;

public record OtpResponse(
        boolean success,
        String message,
        String email,
        long expiresInSeconds,
        String debugOtp
) {
}
