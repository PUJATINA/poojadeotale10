package com.ecommerse.backend.repository;

import com.ecommerse.backend.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {
}
