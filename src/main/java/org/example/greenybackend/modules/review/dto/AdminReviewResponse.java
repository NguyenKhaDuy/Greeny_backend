package org.example.greenybackend.modules.review.dto;

import java.time.LocalDateTime;

public record AdminReviewResponse(
        String reviewId,
        Integer rating,
        String title,
        String comment,
        String images,
        Boolean isApproved,
        Integer helpfulCount,
        String orderId,
        String plantId,
        String plantTitle,
        String customerId,
        String customerName,
        String customerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
