package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.AuthResponse;
import com.ecommerse.backend.dto.ForgotPasswordRequest;
import com.ecommerse.backend.dto.LoginRequest;
import com.ecommerse.backend.dto.LoginOtpVerifyRequest;
import com.ecommerse.backend.dto.MessageResponse;
import com.ecommerse.backend.dto.OtpResponse;
import com.ecommerse.backend.dto.RegisterRequest;
import com.ecommerse.backend.dto.RegisterOtpVerifyRequest;
import com.ecommerse.backend.dto.ResetPasswordOtpVerifyRequest;
import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.entity.UserRole;
import com.ecommerse.backend.repository.UserRepository;
import com.ecommerse.backend.security.AppUserPrincipal;
import com.ecommerse.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    @Value("${app.auth.otp.ttl-seconds:300}")
    private long otpTtlSeconds;

    @Value("${app.auth.mail.from:no-reply@shopsphere.local}")
    private String fromEmail;

    @Value("${app.auth.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.auth.otp.return-in-response:true}")
    private boolean includeDebugOtpInResponse;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            JwtService jwtService,
            TokenSessionService tokenSessionService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
    }

    public OtpResponse requestRegisterOtp(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        log.info("Register OTP requested for email={}", maskEmail(normalizedEmail));

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Register OTP rejected because email already exists: {}", maskEmail(normalizedEmail));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(otpTtlSeconds);
        otpStore.put(otpKey("REGISTER", normalizedEmail), new OtpRecord(otp, expiresAt));

        sendOtpEmail(normalizedEmail, otp, "registration");
        log.info("Register OTP generated for email={} expiresIn={}s", maskEmail(normalizedEmail), otpTtlSeconds);

        return new OtpResponse(
                true,
                "OTP sent to your email for registration verification.",
                normalizedEmail,
                otpTtlSeconds,
                includeDebugOtpInResponse ? otp : null
        );
    }

    public AuthSession verifyRegisterOtp(RegisterOtpVerifyRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        log.info("Register OTP verification attempted for email={}", maskEmail(normalizedEmail));

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Register verification rejected because email already exists: {}", maskEmail(normalizedEmail));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        validateOtp("REGISTER", normalizedEmail, request.otp());

        User user = new User();
        user.setName(request.name());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        User savedUser = userRepository.save(user);
        log.info("User registered successfully userId={} email={}", savedUser.getId(), maskEmail(savedUser.getEmail()));
        JwtService.TokenPair tokenPair = jwtService.generateTokenPair(savedUser);
        tokenSessionService.storeRefreshToken(
                savedUser.getId(),
                tokenPair.refreshJti(),
                tokenPair.refreshToken(),
                tokenPair.refreshExpiresAt()
        );
        log.info("JWT returned for registered user userId={} email={}", savedUser.getId(), maskEmail(savedUser.getEmail()));

        AuthResponse response = new AuthResponse(
                true,
                "Registration successful",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                null,
                null
        );
        return new AuthSession(response, tokenPair);
    }

    public OtpResponse requestLoginOtp(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        log.info("Login OTP requested for email={}", maskEmail(normalizedEmail));

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login OTP rejected due to invalid password for email={}", maskEmail(normalizedEmail));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(otpTtlSeconds);
        otpStore.put(otpKey("LOGIN", normalizedEmail), new OtpRecord(otp, expiresAt));

        sendOtpEmail(normalizedEmail, otp, "login");
        log.info("Login OTP generated for userId={} email={} expiresIn={}s", user.getId(), maskEmail(normalizedEmail), otpTtlSeconds);

        return new OtpResponse(
                true,
                "OTP sent to your email for login verification.",
                normalizedEmail,
                otpTtlSeconds,
                includeDebugOtpInResponse ? otp : null
        );
    }

    public AuthSession verifyLoginOtp(LoginOtpVerifyRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        log.info("Login OTP verification attempted for email={}", maskEmail(normalizedEmail));

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login verification rejected due to invalid password for email={}", maskEmail(normalizedEmail));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        validateOtp("LOGIN", normalizedEmail, request.otp());
        log.info("Login successful for userId={} email={}", user.getId(), maskEmail(normalizedEmail));
        JwtService.TokenPair tokenPair = jwtService.generateTokenPair(user);
        tokenSessionService.storeRefreshToken(
                user.getId(),
                tokenPair.refreshJti(),
                tokenPair.refreshToken(),
                tokenPair.refreshExpiresAt()
        );
        log.info("JWT returned for logged in user userId={} email={}", user.getId(), maskEmail(normalizedEmail));

        AuthResponse response = new AuthResponse(
                true,
                "Login successful",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                null,
                null
        );
        return new AuthSession(response, tokenPair);
    }

    public AuthSession refreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        try {
            Claims claims = jwtService.decodeClaims(refreshToken);
            Long userId = jwtService.extractUid(claims);
            String refreshJti = jwtService.extractJti(claims);
            if (userId == null || refreshJti == null || refreshJti.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token claims");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

            if (!jwtService.validateRefreshClaims(claims, user, refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            Optional<com.ecommerse.backend.entity.RefreshToken> storedTokenOpt = tokenSessionService.findRefreshToken(refreshJti);
            if (storedTokenOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not recognized");
            }

            com.ecommerse.backend.entity.RefreshToken storedToken = storedTokenOpt.get();
            if (storedToken.isRevoked()) {
                tokenSessionService.revokeAllActiveRefreshTokens(userId, refreshJti);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
            }

            if (Instant.now().isAfter(storedToken.getExpiresAt())) {
                tokenSessionService.revokeRefreshToken(storedToken, "expired");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
            }

            if (!tokenSessionService.refreshTokenMatches(storedToken, refreshToken)) {
                tokenSessionService.revokeAllActiveRefreshTokens(userId, refreshJti);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected");
            }

            tokenSessionService.touchRefreshToken(storedToken);
            JwtService.TokenPair rotatedPair = jwtService.rotateTokenPair(user);
            tokenSessionService.revokeRefreshToken(storedToken, rotatedPair.refreshJti());
            tokenSessionService.storeRefreshToken(
                    user.getId(),
                    rotatedPair.refreshJti(),
                    rotatedPair.refreshToken(),
                    rotatedPair.refreshExpiresAt()
            );

            AuthResponse response = new AuthResponse(
                    true,
                    "Session refreshed",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name(),
                    null,
                    null
            );
            return new AuthSession(response, rotatedPair);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Refresh failed due to invalid JWT claims");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims claims = jwtService.decodeClaims(refreshToken);
                String refreshJti = jwtService.extractJti(claims);
                tokenSessionService.findRefreshToken(refreshJti)
                        .ifPresent(token -> tokenSessionService.revokeRefreshToken(token, "logout"));
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Logout refresh token decode failed");
            }
        }

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtService.decodeClaims(accessToken);
                String accessJti = jwtService.extractJti(claims);
                Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
                tokenSessionService.blacklistAccessToken(accessJti, expiresAt, "logout");
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Logout access token decode failed");
            }
        }
    }

    public AuthResponse currentUser(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return new AuthResponse(
                true,
                "Authenticated user",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                null,
                null
        );
    }

    public OtpResponse requestPasswordResetOtp(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        log.info("Password reset OTP requested for email={}", maskEmail(normalizedEmail));
        userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email is not registered"));

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(otpTtlSeconds);
        otpStore.put(otpKey("RESET", normalizedEmail), new OtpRecord(otp, expiresAt));

        sendOtpEmail(normalizedEmail, otp, "password reset");
        log.info("Password reset OTP generated for email={} expiresIn={}s", maskEmail(normalizedEmail), otpTtlSeconds);

        return new OtpResponse(
                true,
                "OTP sent to your email for password reset.",
                normalizedEmail,
                otpTtlSeconds,
                includeDebugOtpInResponse ? otp : null
        );
    }

    public MessageResponse verifyPasswordResetOtp(ResetPasswordOtpVerifyRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        log.info("Password reset OTP verification attempted for email={}", maskEmail(normalizedEmail));
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email is not registered"));

        validateOtp("RESET", normalizedEmail, request.otp());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password reset successful for userId={} email={}", user.getId(), maskEmail(normalizedEmail));

        return new MessageResponse(true, "Password reset successful. Please login with your new password.");
    }

    private void validateOtp(String purpose, String email, String otp) {
        OtpRecord record = otpStore.get(otpKey(purpose, email));
        if (record == null) {
            log.warn("OTP validation failed: missing record for purpose={} email={}", purpose, maskEmail(email));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP not requested or expired");
        }

        if (Instant.now().isAfter(record.expiresAt())) {
            otpStore.remove(otpKey(purpose, email));
            log.warn("OTP validation failed: expired OTP for purpose={} email={}", purpose, maskEmail(email));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired. Please request a new OTP.");
        }

        if (!record.otp().equals(otp)) {
            log.warn("OTP validation failed: invalid OTP for purpose={} email={}", purpose, maskEmail(email));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        otpStore.remove(otpKey(purpose, email));
        log.info("OTP validation success for purpose={} email={}", purpose, maskEmail(email));
    }

    private String generateOtp() {
        int value = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    private String otpKey(String purpose, String email) {
        return purpose + ":" + email;
    }

    private void sendOtpEmail(String toEmail, String otp, String purpose) {
        if (!mailEnabled) {
            log.info("Mail disabled. Skipping OTP email send for purpose={} email={}", purpose, maskEmail(toEmail));
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ShopSphere " + purpose + " OTP");
            message.setText("Your ShopSphere OTP is " + otp + ". It is valid for " + otpTtlSeconds + " seconds.");
            mailSender.send(message);
            log.info("OTP email sent for purpose={} email={}", purpose, maskEmail(toEmail));
        } catch (Exception ex) {
            log.error("OTP email sending failed for purpose={} email={}", purpose, maskEmail(toEmail), ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send OTP email. Check SMTP configuration and try again."
            );
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "invalid-email";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }

    private record OtpRecord(String otp, Instant expiresAt) {
    }

    public record AuthSession(AuthResponse response, JwtService.TokenPair tokenPair) {
    }
}
