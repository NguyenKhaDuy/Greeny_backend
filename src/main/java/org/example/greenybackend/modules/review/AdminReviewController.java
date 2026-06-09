package org.example.greenybackend.modules.review;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.review.dto.AdminReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewModerationRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final AdminReviewService reviewService;

    public AdminReviewController(AdminReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<AdminReviewResponse> getAll(
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate created
    ) {
        return reviewService.getAllReviews(approved, product, customer, rating, status, created);
    }

    @GetMapping("/{reviewId}")
    public AdminReviewResponse getById(@PathVariable String reviewId) {
        return reviewService.getReview(reviewId);
    }

    @PostMapping("/{reviewId}/moderation")
    public AdminReviewResponse moderate(
            @PathVariable String reviewId,
            @RequestBody ReviewModerationRequest request
    ) {
        return reviewService.moderateReview(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public MessageResponse delete(@PathVariable String reviewId) {
        reviewService.deleteReview(reviewId);
        return new MessageResponse("Da xoa danh gia.");
    }
}
