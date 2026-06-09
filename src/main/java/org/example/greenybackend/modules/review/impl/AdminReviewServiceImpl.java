package org.example.greenybackend.modules.review.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.dateEquals;
import static org.example.greenybackend.common.util.AdminFilters.isBlankOrAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.AdminNotificationService;
import org.example.greenybackend.modules.notification.dto.AdminNotificationRequest;
import org.example.greenybackend.modules.review.AdminReviewService;
import org.example.greenybackend.modules.review.ProductReviewsRepository;
import org.example.greenybackend.modules.review.dto.AdminReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewModerationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ProductReviewsRepository reviewRepository;
    private final AdminNotificationService notificationService;

    public AdminReviewServiceImpl(ProductReviewsRepository reviewRepository, AdminNotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<AdminReviewResponse> getAllReviews(Boolean approved) {
        return getAllReviews(approved, null, null, null, null, null);
    }

    @Override
    public List<AdminReviewResponse> getAllReviews(
            Boolean approved,
            String product,
            String customer,
            Integer rating,
            String status,
            LocalDate created
    ) {
        return reviewRepository.findAll().stream()
                .filter(review -> matchesApproved(review, approved, status))
                .filter(review -> contains(review.getPlant() == null ? null : review.getPlant().getTitle(), product))
                .filter(review -> matchesCustomer(review, customer))
                .filter(review -> rating == null || rating.equals(review.getRating()))
                .filter(review -> dateEquals(review.getCreatedAt(), created))
                .sorted(Comparator.comparing(ProductReviews::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminReviewResponse getReview(String reviewId) {
        return toResponse(findReview(reviewId));
    }

    @Transactional
    @Override
    public AdminReviewResponse moderateReview(String reviewId, ReviewModerationRequest request) {
        ProductReviews review = findReview(reviewId);
        if (request.isApproved() != null) {
            review.setIsApproved(request.isApproved());
            review.setUpdatedAt(LocalDateTime.now());
        }
        if (request.replyMessage() != null && !request.replyMessage().isBlank()) {
            sendReplyNotification(review, request.replyMessage());
        }
        return toResponse(review);
    }

    @Transactional
    @Override
    public void deleteReview(String reviewId) {
        reviewRepository.delete(findReview(reviewId));
    }

    private boolean matchesApproved(ProductReviews review, Boolean approved, String status) {
        if (approved != null) {
            return approved.equals(review.getIsApproved());
        }
        if (isBlankOrAll(status)) {
            return true;
        }
        String normalizedStatus = status.trim().toLowerCase();
        return switch (normalizedStatus) {
            case "approved", "visible", "true" -> Boolean.TRUE.equals(review.getIsApproved());
            case "pending" -> review.getIsApproved() == null;
            case "rejected", "hidden", "false" -> Boolean.FALSE.equals(review.getIsApproved());
            default -> true;
        };
    }

    private boolean matchesCustomer(ProductReviews review, String customer) {
        UserEntity user = review.getUserEntity();
        String customerText = user == null ? "" : ((user.getTitle() == null ? "" : user.getTitle())
                + " "
                + (user.getEmail() == null ? "" : user.getEmail()));
        return contains(customerText, customer);
    }

    private void sendReplyNotification(ProductReviews review, String message) {
        UserEntity user = review.getUserEntity();
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Danh gia chua co nguoi dung de phan hoi");
        }
        Plant plant = review.getPlant();
        String plantTitle = plant == null || plant.getTitle() == null ? "san pham" : plant.getTitle();
        notificationService.sendNotification(new AdminNotificationRequest(
                "USER",
                null,
                user.getUserId(),
                null,
                3,
                "Phản hồi đánh giá của bạn",
                message.trim(),
                "{\"reviewId\":\"" + review.getProductReviewsId() + "\",\"plantTitle\":\"" + escapeJson(plantTitle) + "\"}"
        ));
    }

    private ProductReviews findReview(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
    }

    private AdminReviewResponse toResponse(ProductReviews review) {
        Plant plant = review.getPlant();
        UserEntity user = review.getUserEntity();
        Orders order = review.getOrders();
        return new AdminReviewResponse(
                review.getProductReviewsId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getImages(),
                review.getIsApproved(),
                review.getHelpfulCount(),
                order == null ? null : order.getOrderId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                user == null ? null : user.getUserId(),
                user == null ? null : user.getTitle(),
                user == null ? null : user.getEmail(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
