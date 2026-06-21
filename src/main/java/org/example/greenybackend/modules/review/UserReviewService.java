package org.example.greenybackend.modules.review;

import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.review.dto.ReviewCreateRequest;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserReviewService {

    List<ReviewResponse> getMyReviews(UserEntity user);

    ReviewResponse createReview(UserEntity user, ReviewCreateRequest request);

    ReviewResponse createReview(UserEntity user, ReviewCreateRequest request, MultipartFile imageFile);

    ReviewResponse updateReview(UserEntity user, String reviewId, ReviewUpdateRequest request);

    ReviewResponse updateReview(UserEntity user, String reviewId, ReviewUpdateRequest request, MultipartFile imageFile);

    void deleteReview(UserEntity user, String reviewId);

}
