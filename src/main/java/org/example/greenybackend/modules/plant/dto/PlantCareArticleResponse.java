package org.example.greenybackend.modules.plant.dto;

import java.time.LocalDateTime;

public record PlantCareArticleResponse(
        String plantCareArticlesId,
        String title,
        String slug,
        String excerpt,
        String content,
        String thumbnail,
        String authorId,
        String authorName,
        String authorEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
