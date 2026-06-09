package org.example.greenybackend.modules.user.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String userId,
        String email,
        String title,
        String phone,
        String avatar,
        Integer role,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
