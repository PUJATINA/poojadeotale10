package com.ecommerse.backend.service;

import com.ecommerse.backend.entity.RefreshToken;
import com.ecommerse.backend.entity.RevokedAccessToken;
import com.ecommerse.backend.repository.RefreshTokenRepository;
import com.ecommerse.backend.repository.RevokedAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class TokenSessionService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public TokenSessionService(
            RefreshTokenRepository refreshTokenRepository,
            RevokedAccessTokenRepository revokedAccessTokenRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Transactional
    public void storeRefreshToken(Long userId, String refreshJti, String refreshTokenValue, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setJti(refreshJti);
        token.setTokenHash(hashToken(refreshTokenValue));
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(Instant.now());
        token.setRevoked(false);
        refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findRefreshToken(String refreshJti) {
        return refreshTokenRepository.findByJti(refreshJti);
    }

    public boolean refreshTokenMatches(RefreshToken refreshToken, String rawToken) {
        return hashToken(rawToken).equals(refreshToken.getTokenHash());
    }

    @Transactional
    public void revokeRefreshToken(RefreshToken refreshToken, String replacedByJti) {
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setReplacedByJti(replacedByJti);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void touchRefreshToken(RefreshToken refreshToken) {
        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllActiveRefreshTokens(Long userId, String reasonJti) {
        for (RefreshToken token : refreshTokenRepository.findByUserIdAndRevokedFalse(userId)) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            token.setReplacedByJti(reasonJti);
            refreshTokenRepository.save(token);
        }
    }

    @Transactional
    public void blacklistAccessToken(String accessJti, Instant expiresAt, String reason) {
        if (accessJti == null || accessJti.isBlank() || expiresAt == null) {
            return;
        }
        if (expiresAt.isBefore(Instant.now())) {
            return;
        }
        RevokedAccessToken revoked = new RevokedAccessToken();
        revoked.setJti(accessJti);
        revoked.setExpiresAt(expiresAt);
        revoked.setRevokedAt(Instant.now());
        revoked.setReason(reason);
        revokedAccessTokenRepository.save(revoked);
    }

    public boolean isAccessTokenRevoked(String accessJti) {
        if (accessJti == null || accessJti.isBlank()) {
            return false;
        }
        return revokedAccessTokenRepository.existsById(accessJti);
    }

    @Transactional
    public StartupClearSummary clearAllTokenStateOnStartup() {
        long refreshCount = refreshTokenRepository.count();
        long revokedAccessCount = revokedAccessTokenRepository.count();
        refreshTokenRepository.deleteAllInBatch();
        revokedAccessTokenRepository.deleteAllInBatch();
        return new StartupClearSummary(refreshCount, revokedAccessCount);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 hashing unavailable", ex);
        }
    }

    public record StartupClearSummary(long clearedRefreshTokens, long clearedRevokedAccessTokens) {
    }
}
