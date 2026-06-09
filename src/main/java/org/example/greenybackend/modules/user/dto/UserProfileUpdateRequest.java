package org.example.greenybackend.modules.user.dto;

public record UserProfileUpdateRequest(
        String title,
        String phone,
        String email,
        String avatar
) {
}
