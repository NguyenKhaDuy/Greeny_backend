package org.example.greenybackend.modules.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminNotificationResponse(
        String notificationId,
        Integer type,
        String title,
        String messageText,
        String data,
        Integer recipientCount,
        LocalDateTime createdAt,
        List<AdminNotificationRecipientResponse> recipients
) {
}
