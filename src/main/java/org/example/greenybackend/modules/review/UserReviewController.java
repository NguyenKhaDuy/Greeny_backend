package org.example.greenybackend.modules.review;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.review.dto.ReviewCreateRequest;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/reviews")
public class UserReviewController {

    private final UserReviewService reviewService;

    public UserReviewController(UserReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewResponse> myReviews(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return reviewService.getMyReviews(currentUser);
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(currentUser, request));
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String reviewId,
            @RequestBody ReviewUpdateRequest request
    ) {
        return reviewService.updateReview(currentUser, reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public MessageResponse deleteReview(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String reviewId
    ) {
        reviewService.deleteReview(currentUser, reviewId);
        return new MessageResponse("Xoa danh gia thanh cong");
    }
}
