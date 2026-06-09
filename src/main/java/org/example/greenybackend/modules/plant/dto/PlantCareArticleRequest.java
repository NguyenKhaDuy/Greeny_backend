package org.example.greenybackend.modules.plant.dto;

public record PlantCareArticleRequest(
        String title,
        String slug,
        String excerpt,
        String content,
        String thumbnail
) {
}
