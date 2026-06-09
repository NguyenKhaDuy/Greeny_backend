package org.example.greenybackend.modules.ai.dto;

public record ChatRequest(
        String message,
        String conversationId
) {
}
