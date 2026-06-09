package org.example.greenybackend.modules.notification.dto;

import java.time.LocalDateTime;

public record UserNotificationResponse(
        String notificationUserId,
        String notificationId,
        Integer type,
        String title,
        String messageText,
        String data,
        Boolean isRead,
        String sendingTime,
        LocalDateTime createdAt
) {
}
