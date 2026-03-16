package com.ecommerse.backend.config;

import com.ecommerse.backend.service.TokenSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupTokenInvalidationRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupTokenInvalidationRunner.class);

    private final TokenSessionService tokenSessionService;

    public StartupTokenInvalidationRunner(TokenSessionService tokenSessionService) {
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public void run(String... args) {
        TokenSessionService.StartupClearSummary summary = tokenSessionService.clearAllTokenStateOnStartup();
        log.warn(
                "Startup token invalidation completed: clearedRefreshTokens={} clearedRevokedAccessTokens={}",
                summary.clearedRefreshTokens(),
                summary.clearedRevokedAccessTokens()
        );
    }
}
