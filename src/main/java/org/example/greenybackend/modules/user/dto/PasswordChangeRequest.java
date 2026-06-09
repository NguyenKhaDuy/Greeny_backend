package org.example.greenybackend.modules.user.dto;

public record PasswordChangeRequest(
        String currentPassword,
        String newPassword
) {
}
