package org.example.greenybackend.modules.category.dto;

public record CategoryRequest(
        String title,
        String description,
        String imageUrl,
        Boolean isActive,
        Integer sortOrder
) {
}
