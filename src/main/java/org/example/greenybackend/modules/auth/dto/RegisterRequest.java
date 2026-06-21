package org.example.greenybackend.modules.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        String fullName,
        String title,
        String phone
) {
}
