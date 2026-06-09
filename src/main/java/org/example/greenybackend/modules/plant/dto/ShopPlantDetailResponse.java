package org.example.greenybackend.modules.plant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.variant.dto.ShopVariantResponse;

public record ShopPlantDetailResponse(
        String plantId,
        String title,
        String sku,
        String description,
        String scientificName,
        String commonName,
        Integer plantType,
        String origin,
        String toxicity,
        Boolean petFriendly,
        Boolean airPurifying,
        String categoryId,
        String categoryTitle,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer totalStock,
        Double averageRating,
        Long totalReviews,
        String imageUrl,
        List<String> images,
        List<ShopVariantResponse> variants,
        List<ReviewResponse> reviews,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
