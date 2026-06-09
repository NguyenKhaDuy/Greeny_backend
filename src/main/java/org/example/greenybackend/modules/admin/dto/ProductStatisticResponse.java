package org.example.greenybackend.modules.admin.dto;

import java.math.BigDecimal;

public record ProductStatisticResponse(
        String statisticLevel,
        String groupKey,
        String groupTitle,
        String plantId,
        String plantTitle,
        String categoryTitle,
        Integer plantType,
        String plantTypeLabel,
        Integer plantCount,
        Integer variantCount,
        Integer stockQuantity,
        Integer soldQuantity,
        BigDecimal revenue,
        BigDecimal averageRating,
        Integer reviewCount
) {
}
