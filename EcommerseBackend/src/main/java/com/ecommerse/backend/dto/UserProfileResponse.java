package com.ecommerse.backend.dto;

public record UserProfileResponse(
        Long userId,
        String name,
        String email,
        String phoneNumber,
        String alternatePhoneNumber,
        Integer age,
        String gender,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        String occupation
) {
}
