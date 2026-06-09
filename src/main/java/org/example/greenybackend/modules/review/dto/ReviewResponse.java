package org.example.greenybackend.modules.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        String reviewId,
        String orderId,
        String plantId,
        String plantTitle,
        String userId,
        String userName,
        Integer rating,
        String title,
        String comment,
        String images,
        Boolean isApproved,
        Integer helpfulCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
