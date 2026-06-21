package org.example.greenybackend.modules.review.dto;

public record ReviewCreateRequest(
        String orderId,
        String plantId,
        Integer rating,
        String title,
        String comment
) {
}
