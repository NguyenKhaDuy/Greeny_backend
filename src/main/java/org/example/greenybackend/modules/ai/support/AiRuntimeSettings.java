package org.example.greenybackend.modules.ai.support;

import java.math.BigDecimal;

public record AiRuntimeSettings(
        String provider,
        String model,
        String systemPrompt,
        String apiKey,
        String endpoint,
        BigDecimal temperature,
        int maxTokens
) {
}
