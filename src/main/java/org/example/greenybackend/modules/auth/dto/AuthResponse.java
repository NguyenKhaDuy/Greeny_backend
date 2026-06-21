package org.example.greenybackend.modules.auth.dto;

import java.time.LocalDateTime;

public record AuthResponse(
        String userId,
        String email,
        String title,
        String fullName,
        Integer role,
        Integer status,
        LocalDateTime emailVerifiedAt,
        String token
) {
}
