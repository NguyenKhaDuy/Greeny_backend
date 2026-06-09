package org.example.greenybackend.modules.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryDTO(
        String conversationId,
        String currentStep,
        Integer status,
        LocalDateTime startedAt,
        LocalDateTime updatedAt,
        List<ChatMessageDTO> messages
) {
}
