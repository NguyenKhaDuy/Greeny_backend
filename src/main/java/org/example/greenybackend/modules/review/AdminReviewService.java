package org.example.greenybackend.modules.review;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.modules.review.dto.AdminReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewModerationRequest;

public interface AdminReviewService {

    List<AdminReviewResponse> getAllReviews(Boolean approved);

    List<AdminReviewResponse> getAllReviews(
            Boolean approved,
            String product,
            String customer,
            Integer rating,
            String status,
            LocalDate created
    );

    AdminReviewResponse getReview(String reviewId);

    AdminReviewResponse moderateReview(String reviewId, ReviewModerationRequest request);

    void deleteReview(String reviewId);

}
