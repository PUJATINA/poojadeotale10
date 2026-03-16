package com.ecommerse.backend.service;

import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.entity.UserRole;
import com.ecommerse.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final String bootstrapEmail;

    public AdminBootstrapService(
            UserRepository userRepository,
            @Value("${app.admin.bootstrap-email:}") String bootstrapEmail
    ) {
        this.userRepository = userRepository;
        this.bootstrapEmail = bootstrapEmail == null ? "" : bootstrapEmail.trim().toLowerCase();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrapAdminRole() {
        for (User user : userRepository.findByRoleIsNull()) {
            user.setRole(UserRole.USER);
            userRepository.save(user);
        }

        if (bootstrapEmail.isBlank()) {
            return;
        }

        userRepository.findByEmail(bootstrapEmail).ifPresentOrElse(user -> {
            if (user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.ADMIN);
                userRepository.save(user);
                log.info("Bootstrap admin role assigned to email={}", maskEmail(bootstrapEmail));
            }
        }, () -> log.warn("Bootstrap admin email not found: {}", maskEmail(bootstrapEmail)));
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
}
