package org.example.greenybackend.modules.review.dto;

public record ReviewModerationRequest(
        Boolean isApproved,
        String replyMessage
) {
}
