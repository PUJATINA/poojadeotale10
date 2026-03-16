package com.ecommerse.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordOtpVerifyRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Email must be in valid format, for example: name@example.com")
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        String otp,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        @Pattern(
                regexp = "^(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$",
                message = "Password must include at least 1 number and 1 special character")
        String newPassword
) {
}
