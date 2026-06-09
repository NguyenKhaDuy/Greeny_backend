package org.example.greenybackend.modules.ai.dto;

import java.time.LocalDateTime;

public record AiUsageLimitDTO(
        String provider,
        String model,
        boolean billingGuardEnabled,
        boolean allowPaidModels,
        int maxOutputTokens,
        int appDailyRequestLimit,
        long usedRequestsToday,
        long remainingRequestsToday,
        int appDailyTokenLimit,
        long usedTokensToday,
        long remainingTokensToday,
        int providerFreeRpm,
        int providerFreeTpm,
        int providerFreeRpd,
        int googleSearchFreeRpd,
        boolean googleSearchGroundingEnabled,
        LocalDateTime resetAt,
        String note
) {
}
