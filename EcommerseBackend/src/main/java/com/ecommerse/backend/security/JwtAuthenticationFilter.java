package com.ecommerse.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ecommerse.backend.service.TokenSessionService;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final TokenSessionService tokenSessionService;
    private final String accessCookieName;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserDetailsService userDetailsService,
            TokenSessionService tokenSessionService,
            @Value("${app.auth.cookie.access-name:ACCESS_TOKEN}") String accessCookieName
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenSessionService = tokenSessionService;
        this.accessCookieName = accessCookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = resolveToken(request);
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            log.debug("JWT bearer token received method={} path={}", request.getMethod(), request.getRequestURI());
            Claims claims = jwtService.decodeClaims(jwt);
            String accessJti = claims.getId();
            if (tokenSessionService.isAccessTokenRevoked(accessJti)) {
                log.warn("JWT denied due to revoked access token method={} path={} jti={}",
                        request.getMethod(), request.getRequestURI(), accessJti);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                AppUserPrincipal principal = (AppUserPrincipal) userDetails;

                if (jwtService.validateClaims(claims, principal, jwt)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT auth success method={} path={} userId={} email={}",
                            request.getMethod(), request.getRequestURI(), principal.getId(), principal.getUsername());
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT auth failed method={} path={} reason={}",
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_PREFIX.length());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (accessCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
