package org.example.greenybackend.modules.user.dto;

public record AdminUserUpdateRequest(
        String title,
        String phone,
        Integer role,
        Integer status
) {
}
