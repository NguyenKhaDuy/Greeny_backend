package org.example.greenybackend.security;

import java.util.Collection;
import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class GreenyUserDetails implements UserDetails {

    private static final int ROLE_ADMIN = 0;
    private static final int STATUS_ACTIVE = 1;

    private final UserEntity user;

    public GreenyUserDetails(UserEntity user) {
        this.user = user;
    }

    public UserEntity getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getRole() != null && user.getRole() == ROLE_ADMIN ? "ROLE_ADMIN" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return user.getPass();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus() == STATUS_ACTIVE;
    }
}
