package com.ecommerse.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
        String phoneNumber,

        @Pattern(regexp = "^[0-9]{10}$", message = "Alternate phone number must be 10 digits")
        String alternatePhoneNumber,

        @Min(value = 10, message = "Age must be at least 10")
        @Max(value = 100, message = "Age must be at most 100")
        Integer age,

        @Pattern(regexp = "^(Male|Female|Other)?$", message = "Gender must be Male, Female, or Other")
        String gender,

        @Size(max = 300, message = "Address line 1 must be at most 300 characters")
        String addressLine1,

        @Size(max = 300, message = "Address line 2 must be at most 300 characters")
        String addressLine2,

        @Size(max = 120, message = "City must be at most 120 characters")
        String city,

        @Size(max = 120, message = "State must be at most 120 characters")
        String state,

        @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
        String pincode,

        @Size(max = 120, message = "Occupation must be at most 120 characters")
        String occupation
) {
}
