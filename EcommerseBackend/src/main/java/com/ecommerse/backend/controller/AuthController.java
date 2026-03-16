package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.AuthResponse;
import com.ecommerse.backend.dto.ForgotPasswordRequest;
import com.ecommerse.backend.dto.LoginRequest;
import com.ecommerse.backend.dto.LoginOtpVerifyRequest;
import com.ecommerse.backend.dto.MessageResponse;
import com.ecommerse.backend.dto.OtpResponse;
import com.ecommerse.backend.dto.RegisterRequest;
import com.ecommerse.backend.dto.RegisterOtpVerifyRequest;
import com.ecommerse.backend.dto.ResetPasswordOtpVerifyRequest;
import com.ecommerse.backend.security.AppUserPrincipal;
import com.ecommerse.backend.security.AuthCookieService;
import com.ecommerse.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register/request-otp")
    public ResponseEntity<OtpResponse> requestRegisterOtp(@Valid @RequestBody RegisterRequest request) {
        log.info("API hit: register/request-otp");
        return ResponseEntity.ok(authService.requestRegisterOtp(request));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<AuthResponse> verifyRegisterOtp(@Valid @RequestBody RegisterOtpVerifyRequest request) {
        log.info("API hit: register/verify-otp");
        AuthService.AuthSession session = authService.verifyRegisterOtp(request);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookies(headers, session.tokenPair().accessToken(), session.tokenPair().refreshToken());
        return ResponseEntity.ok().headers(headers).body(session.response());
    }

    @PostMapping("/login/request-otp")
    public ResponseEntity<OtpResponse> requestLoginOtp(@Valid @RequestBody LoginRequest request) {
        log.info("API hit: login/request-otp");
        return ResponseEntity.ok(authService.requestLoginOtp(request));
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<AuthResponse> verifyLoginOtp(@Valid @RequestBody LoginOtpVerifyRequest request) {
        log.info("API hit: login/verify-otp");
        AuthService.AuthSession session = authService.verifyLoginOtp(request);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookies(headers, session.tokenPair().accessToken(), session.tokenPair().refreshToken());
        return ResponseEntity.ok().headers(headers).body(session.response());
    }

    @PostMapping("/password/request-otp")
    public ResponseEntity<OtpResponse> requestPasswordResetOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("API hit: password/request-otp");
        return ResponseEntity.ok(authService.requestPasswordResetOtp(request));
    }

    @PostMapping("/password/verify-otp")
    public ResponseEntity<MessageResponse> verifyPasswordResetOtp(
            @Valid @RequestBody ResetPasswordOtpVerifyRequest request) {
        log.info("API hit: password/verify-otp");
        return ResponseEntity.ok(authService.verifyPasswordResetOtp(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        log.info("API hit: refresh");
        String refreshToken = findCookieValue(request, authCookieService.getRefreshCookieName());
        AuthService.AuthSession session = authService.refreshSession(refreshToken);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookies(headers, session.tokenPair().accessToken(), session.tokenPair().refreshToken());
        return ResponseEntity.ok().headers(headers).body(session.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        log.info("API hit: logout");
        String refreshToken = findCookieValue(request, authCookieService.getRefreshCookieName());
        String accessToken = findCookieValue(request, authCookieService.getAccessCookieName());
        authService.logout(accessToken, refreshToken);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.clearAuthCookies(headers);
        return ResponseEntity.ok().headers(headers).body(new MessageResponse(true, "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return ResponseEntity.ok(authService.currentUser(principal));
    }

    private String findCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
