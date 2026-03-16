package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.UpdateUserProfileRequest;
import com.ecommerse.backend.dto.UserProfileResponse;
import com.ecommerse.backend.security.AppUserPrincipal;
import com.ecommerse.backend.service.UserProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable Long userId, @AuthenticationPrincipal AppUserPrincipal principal) {
        validateUserAccess(userId, principal);
        log.info("API hit: get profile userId={}", userId);
        return userProfileService.getProfile(userId);
    }

    @PutMapping("/{userId}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        validateUserAccess(userId, principal);
        log.info("API hit: update profile userId={}", userId);
        return userProfileService.updateProfile(userId, request);
    }

    private void validateUserAccess(Long requestedUserId, AppUserPrincipal principal) {
        if (!principal.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for requested user");
        }
    }
}
