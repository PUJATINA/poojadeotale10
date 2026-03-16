package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.UpdateUserProfileRequest;
import com.ecommerse.backend.dto.UserProfileResponse;
import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(Long userId) {
        log.info("Profile fetch requested for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Profile fetch failed: user not found userId={}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });
        log.info("Profile fetch successful for userId={}", userId);
        return toResponse(user);
    }

    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        log.info("Profile update requested for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Profile update failed: user not found userId={}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().trim());
        }
        if (request.alternatePhoneNumber() != null) {
            user.setAlternatePhoneNumber(request.alternatePhoneNumber().trim());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        if (request.gender() != null) {
            user.setGender(request.gender().trim());
        }
        if (request.addressLine1() != null) {
            user.setAddressLine1(request.addressLine1().trim());
        }
        if (request.addressLine2() != null) {
            user.setAddressLine2(request.addressLine2().trim());
        }
        if (request.city() != null) {
            user.setCity(request.city().trim());
        }
        if (request.state() != null) {
            user.setState(request.state().trim());
        }
        if (request.pincode() != null) {
            user.setPincode(request.pincode().trim());
        }
        if (request.occupation() != null) {
            user.setOccupation(request.occupation().trim());
        }

        User savedUser = userRepository.save(user);
        log.info("Profile update successful for userId={}", userId);
        return toResponse(savedUser);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAlternatePhoneNumber(),
                user.getAge(),
                user.getGender(),
                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getCity(),
                user.getState(),
                user.getPincode(),
                user.getOccupation()
        );
    }
}
