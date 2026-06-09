package org.example.greenybackend.modules.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        String title,
        String phone,
        String avatar
) {
}
