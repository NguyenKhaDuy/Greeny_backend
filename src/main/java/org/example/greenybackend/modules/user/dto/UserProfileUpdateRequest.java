package org.example.greenybackend.modules.user.dto;

public record UserProfileUpdateRequest(
        String fullName,
        String title,
        String phone,
        String email
) {
}
