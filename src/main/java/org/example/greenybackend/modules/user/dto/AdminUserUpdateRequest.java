package org.example.greenybackend.modules.user.dto;

public record AdminUserUpdateRequest(
        String fullName,
        String title,
        String phone,
        Integer role,
        Integer status
) {
}
