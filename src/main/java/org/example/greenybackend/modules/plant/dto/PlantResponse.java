package org.example.greenybackend.modules.plant.dto;

import java.time.LocalDateTime;

public record PlantResponse(
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
