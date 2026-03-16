package com.ecommerse.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
    private final String accessCookieName;
    private final String refreshCookieName;
    private final boolean secureCookie;
    private final String sameSite;
    private final long accessMaxAgeSeconds;
    private final long refreshMaxAgeSeconds;

    public AuthCookieService(
            @Value("${app.auth.cookie.access-name:ACCESS_TOKEN}") String accessCookieName,
            @Value("${app.auth.cookie.refresh-name:REFRESH_TOKEN}") String refreshCookieName,
            @Value("${app.auth.cookie.secure:true}") boolean secureCookie,
            @Value("${app.auth.cookie.same-site:Strict}") String sameSite,
            @Value("${app.auth.jwt.access-expiration-seconds:900}") long accessMaxAgeSeconds,
            @Value("${app.auth.jwt.refresh-expiration-seconds:604800}") long refreshMaxAgeSeconds
    ) {
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.accessMaxAgeSeconds = accessMaxAgeSeconds;
        this.refreshMaxAgeSeconds = refreshMaxAgeSeconds;
    }

    public void addAuthCookies(HttpHeaders headers, String accessToken, String refreshToken) {
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(accessCookieName, accessToken, accessMaxAgeSeconds).toString());
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(refreshCookieName, refreshToken, refreshMaxAgeSeconds).toString());
    }

    public void clearAuthCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(accessCookieName, "", 0).toString());
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(refreshCookieName, "", 0).toString());
    }

    public String getAccessCookieName() {
        return accessCookieName;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite(sameSite)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
