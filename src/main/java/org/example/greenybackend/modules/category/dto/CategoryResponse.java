package org.example.greenybackend.modules.category.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        String caId,
        String title,
        String description,
        String imageUrl,
        Boolean isActive,
        Integer sortOrder,
        LocalDateTime createdCa,
        LocalDateTime updatedCa
) {
}
