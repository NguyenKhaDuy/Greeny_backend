package org.example.greenybackend.modules.review.dto;

public record ReviewUpdateRequest(
        Integer rating,
        String title,
        String comment,
        String images
) {
}
