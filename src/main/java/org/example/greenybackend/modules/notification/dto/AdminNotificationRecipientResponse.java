package org.example.greenybackend.modules.notification.dto;

public record AdminNotificationRecipientResponse(
        String notificationUserId,
        String userId,
        String userName,
        String email,
        Boolean isRead,
        String sendingTime
) {
}
