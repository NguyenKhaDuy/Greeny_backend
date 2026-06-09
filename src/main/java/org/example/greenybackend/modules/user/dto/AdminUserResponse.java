package org.example.greenybackend.modules.user.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        String userId,
        String title,
        String email,
        String phone,
        String avatar,
        Integer role,
        String roleLabel,
        Integer status,
        String statusLabel,
        LocalDateTime emailVerifiedAt,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
