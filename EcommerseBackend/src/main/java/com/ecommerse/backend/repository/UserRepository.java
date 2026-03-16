package com.ecommerse.backend.repository;

import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
    List<User> findByRoleIsNull();
}
