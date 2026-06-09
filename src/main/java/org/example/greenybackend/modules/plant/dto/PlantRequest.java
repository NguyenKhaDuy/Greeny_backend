package org.example.greenybackend.modules.plant.dto;

public record PlantRequest(
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
        String categoryId
) {
}
