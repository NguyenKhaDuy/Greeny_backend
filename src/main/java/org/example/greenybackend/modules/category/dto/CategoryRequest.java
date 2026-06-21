package org.example.greenybackend.modules.category.dto;

public record CategoryRequest(
        String title,
        String description,
        Boolean isActive,
        Integer sortOrder
) {
}
