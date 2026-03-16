package com.ecommerse.backend.security;

import com.ecommerse.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;
    private final Instant serverStartedAt;

    public JwtService(
            @Value("${app.auth.jwt.ephemeral-signing-key:true}") boolean ephemeralSigningKey,
            @Value("${app.auth.jwt.secret:}") String jwtSecret,
            @Value("${app.auth.jwt.access-expiration-seconds:900}") long accessExpirationSeconds,
            @Value("${app.auth.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds
    ) {
        this.serverStartedAt = Instant.now();
        if (ephemeralSigningKey) {
            byte[] randomKey = new byte[64];
            new SecureRandom().nextBytes(randomKey);
            this.signingKey = Keys.hmacShaKeyFor(randomKey);
            log.warn("JWT service started with ephemeral signing key. All previous JWTs are invalid after restart.");
        } else {
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException("app.auth.jwt.secret must be configured when ephemeral signing key is disabled");
            }
            this.signingKey = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret));
            log.warn("JWT service started with configured static signing key.");
        }
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        log.info("JWT server startup reference timestamp={}", serverStartedAt);
    }

    public TokenPair generateTokenPair(User user) {
        Instant now = Instant.now();
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        Instant accessExpiresAt = now.plusSeconds(accessExpirationSeconds);
        Instant refreshExpiresAt = now.plusSeconds(refreshExpirationSeconds);

        String accessToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("type", "access")
                .id(accessJti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiresAt))
                .signWith(signingKey)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("type", "refresh")
                .id(refreshJti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiresAt))
                .signWith(signingKey)
                .compact();

        log.info("JWT pair issued userId={} email={} accessExp={}s refreshExp={}s accessRef={} refreshRef={}",
                user.getId(),
                maskEmail(user.getEmail()),
                accessExpirationSeconds,
                refreshExpirationSeconds,
                tokenRef(accessToken),
                tokenRef(refreshToken));
        return new TokenPair(accessToken, refreshToken, accessJti, refreshJti, accessExpiresAt, refreshExpiresAt);
    }

    public TokenPair rotateTokenPair(User user) {
        return generateTokenPair(user);
    }

    public String extractUsername(String token) {
        String username = extractAllClaims(token).getSubject();
        log.debug("JWT subject extracted subject={} tokenRef={}", maskEmail(username), tokenRef(token));
        return username;
    }

    public boolean isTokenValid(String token, AppUserPrincipal principal) {
        String username = extractUsername(token);
        boolean subjectMatches = username.equals(principal.getUsername());
        boolean expired = isTokenExpired(token);
        boolean isValid = subjectMatches && !expired;
        if (!isValid) {
            log.warn("JWT validation failed userId={} subjectMatch={} expired={} tokenRef={}",
                    principal.getId(), subjectMatches, expired, tokenRef(token));
        }
        return isValid;
    }

    public Claims decodeClaims(String token) {
        Claims claims = extractAllClaims(token);
        log.debug("JWT decode method=claims sub={} uid={} issuedAt={} exp={} tokenRef={}",
                maskEmail(claims.getSubject()),
                claims.get("uid"),
                claims.getIssuedAt(),
                claims.getExpiration(),
                tokenRef(token));
        return claims;
    }

    public boolean validateClaims(Claims claims, AppUserPrincipal principal, String token) {
        String subject = claims.getSubject();
        Long uid = toLong(claims.get("uid"));
        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();
        String tokenType = toStringValue(claims.get("type"));
        boolean subjectMatches = Objects.equals(subject, principal.getUsername());
        boolean uidMatches = Objects.equals(uid, principal.getId());
        boolean expired = expiration == null || expiration.before(new Date());
        boolean issuedAfterStartup = issuedAt != null && !issuedAt.toInstant().isBefore(serverStartedAt);
        boolean typeValid = "access".equals(tokenType);
        boolean valid = subjectMatches && uidMatches && !expired && typeValid && issuedAfterStartup;

        if (valid) {
            log.debug("JWT claim validation success userId={} subMatch={} uidMatch={} type={} expired={} issuedAfterStartup={} tokenRef={}",
                    principal.getId(), subjectMatches, uidMatches, tokenType, expired, issuedAfterStartup, tokenRef(token));
        } else {
            log.warn("JWT claim validation failed principalUserId={} claimUid={} subMatch={} uidMatch={} type={} typeValid={} expired={} issuedAfterStartup={} iat={} exp={} tokenRef={}",
                    principal.getId(), uid, subjectMatches, uidMatches, tokenType, typeValid, expired, issuedAfterStartup, issuedAt, expiration, tokenRef(token));
        }
        return valid;
    }

    public boolean validateRefreshClaims(Claims claims, User user, String token) {
        String subject = claims.getSubject();
        Long uid = toLong(claims.get("uid"));
        String tokenType = toStringValue(claims.get("type"));
        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();
        boolean subjectMatches = Objects.equals(subject, user.getEmail());
        boolean uidMatches = Objects.equals(uid, user.getId());
        boolean typeValid = "refresh".equals(tokenType);
        boolean expired = expiration == null || expiration.before(new Date());
        boolean issuedAfterStartup = issuedAt != null && !issuedAt.toInstant().isBefore(serverStartedAt);
        boolean valid = subjectMatches && uidMatches && typeValid && !expired && issuedAfterStartup;

        if (valid) {
            log.debug("JWT refresh claim validation success userId={} type={} issuedAfterStartup={} tokenRef={}",
                    user.getId(), tokenType, issuedAfterStartup, tokenRef(token));
        } else {
            log.warn("JWT refresh claim validation failed userId={} claimUid={} subMatch={} uidMatch={} type={} typeValid={} expired={} issuedAfterStartup={} iat={} tokenRef={}",
                    user.getId(), uid, subjectMatches, uidMatches, tokenType, typeValid, expired, issuedAfterStartup, issuedAt, tokenRef(token));
        }
        return valid;
    }

    public Long extractUid(Claims claims) {
        return toLong(claims.get("uid"));
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public Instant getAccessExpirationInstant() {
        return Instant.now().plusSeconds(accessExpirationSeconds);
    }

    public Instant getRefreshExpirationInstant() {
        return Instant.now().plusSeconds(refreshExpirationSeconds);
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String tokenRef(String token) {
        if (token == null || token.isBlank()) {
            return "empty";
        }
        int prefixLength = Math.min(12, token.length());
        return token.substring(0, prefixLength) + "...(" + token.length() + ")";
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

    public record TokenPair(
            String accessToken,
            String refreshToken,
            String accessJti,
            String refreshJti,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {
    }
}
