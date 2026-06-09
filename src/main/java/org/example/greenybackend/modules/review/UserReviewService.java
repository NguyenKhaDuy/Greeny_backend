package org.example.greenybackend.modules.review;

import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.review.dto.ReviewCreateRequest;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewUpdateRequest;

public interface UserReviewService {

    List<ReviewResponse> getMyReviews(UserEntity user);

    ReviewResponse createReview(UserEntity user, ReviewCreateRequest request);

    ReviewResponse updateReview(UserEntity user, String reviewId, ReviewUpdateRequest request);

    void deleteReview(UserEntity user, String reviewId);

}
