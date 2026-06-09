package org.example.greenybackend.modules.ai;

public record AiGenerationResult(
        String content,
        String provider,
        String model,
        boolean databaseFallback,
        Integer promptTokens,
        Integer completionTokens
) {
}
