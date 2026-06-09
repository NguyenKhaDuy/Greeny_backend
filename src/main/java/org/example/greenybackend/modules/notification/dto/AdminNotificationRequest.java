package org.example.greenybackend.modules.notification.dto;

public record AdminNotificationRequest(
        String targetType,
        Integer role,
        String userId,
        String userEmail,
        Integer type,
        String title,
        String messageText,
        String data
) {
}
