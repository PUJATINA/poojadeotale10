package com.ecommerse.backend.security;

import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {
    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final UserRole role;

    public AppUserPrincipal(Long id, String name, String email, String password, UserRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role == null ? UserRole.USER : role;
    }

    public static AppUserPrincipal fromUser(User user) {
        return new AppUserPrincipal(user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole());
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == UserRole.ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
