package org.example.greenybackend.modules.plant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShopPlantSummaryResponse(
        String plantId,
        String title,
        String description,
        String categoryId,
        String categoryTitle,
        Integer plantType,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer totalStock,
        Double averageRating,
        Long totalReviews,
        String imageUrl,
        LocalDateTime createdAt
) {
}
