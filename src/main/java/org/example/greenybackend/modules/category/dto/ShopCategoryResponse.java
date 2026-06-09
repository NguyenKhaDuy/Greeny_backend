package org.example.greenybackend.modules.category.dto;

public record ShopCategoryResponse(
        String categoryId,
        String title,
        String description,
        String imageUrl,
        Integer sortOrder
) {
}
