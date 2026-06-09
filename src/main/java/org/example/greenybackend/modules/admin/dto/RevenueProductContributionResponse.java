package org.example.greenybackend.modules.admin.dto;

import java.math.BigDecimal;

public record RevenueProductContributionResponse(
        String plantId,
        String plantTitle,
        String categoryTitle,
        Integer soldQuantity,
        Integer stockQuantity,
        BigDecimal revenue,
        Integer contributionPercent
) {
}
