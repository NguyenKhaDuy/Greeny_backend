package org.example.greenybackend.modules.ai.dto;

public record ChatResponse(
        String conversationId,
        ChatMessageDTO userMessage,
        ChatMessageDTO aiMessage,
        String intent,
        AiContextDTO context,
        String provider,
        String model,
        boolean databaseFallback,
        AiUsageLimitDTO usageLimits
) {
}
