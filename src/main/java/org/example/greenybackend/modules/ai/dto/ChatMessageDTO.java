package org.example.greenybackend.modules.ai.dto;

import java.time.LocalDateTime;

public record ChatMessageDTO(
        String messageId,
        String conversationId,
        String senderType,
        String messageText,
        String intent,
        LocalDateTime createdAt
) {
}
