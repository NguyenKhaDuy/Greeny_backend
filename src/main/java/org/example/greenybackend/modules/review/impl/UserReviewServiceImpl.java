package org.example.greenybackend.modules.review.impl;

import org.example.greenybackend.common.util.ImageStorageService;
import org.example.greenybackend.common.util.ImageStorageService.StoredImage;
import org.example.greenybackend.modules.review.UserReviewService;
import org.example.greenybackend.modules.review.ProductReviewsRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.OrderItems;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.UserNotificationService;
import org.example.greenybackend.modules.order.UserOrderService;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.example.greenybackend.modules.product.ShopCatalogService;
import org.example.greenybackend.modules.review.dto.ReviewCreateRequest;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserReviewServiceImpl implements UserReviewService {

    private final ProductReviewsRepository reviewRepository;
    private final PlantRepository plantRepository;
    private final UserOrderService orderService;
    private final ShopCatalogService catalogService;
    private final UserNotificationService notificationService;
    private final ImageStorageService imageStorageService;

    public UserReviewServiceImpl(
            ProductReviewsRepository reviewRepository,
            PlantRepository plantRepository,
            UserOrderService orderService,
            ShopCatalogService catalogService,
            UserNotificationService notificationService,
            ImageStorageService imageStorageService
    ) {
        this.reviewRepository = reviewRepository;
        this.plantRepository = plantRepository;
        this.orderService = orderService;
        this.catalogService = catalogService;
        this.notificationService = notificationService;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getMyReviews(UserEntity user) {
        return reviewRepository.findByUserEntityUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(catalogService::toReviewResponse)
                .toList();
    }

    @Transactional
    @Override
    public ReviewResponse createReview(UserEntity user, ReviewCreateRequest request) {
        return createReview(user, request, null);
    }

    @Transactional
    @Override
    public ReviewResponse createReview(UserEntity user, ReviewCreateRequest request, MultipartFile imageFile) {
        validateReviewFields(request == null ? null : request.rating(), request == null ? null : request.comment());
        if (request.orderId() == null || request.orderId().isBlank()) {
            throw new IllegalArgumentException("Can chon don hang da mua");
        }
        if (request.plantId() == null || request.plantId().isBlank()) {
            throw new IllegalArgumentException("Can chon san pham can danh gia");
        }

        Orders order = orderService.findUserOrder(user, request.orderId());
        if (order.getStatus() == null || order.getStatus() != UserOrderService.ORDER_DELIVERED) {
            throw new IllegalArgumentException("Chi co the danh gia don hang da giao");
        }
        if (!orderContainsPlant(order, request.plantId())) {
            throw new IllegalArgumentException("San pham khong nam trong don hang da mua");
        }
        if (reviewRepository.existsByUserEntityUserIdAndOrdersOrderIdAndPlantPlantId(user.getUserId(), order.getOrderId(), request.plantId())) {
            throw new IllegalArgumentException("Ban da danh gia san pham nay trong don hang nay");
        }

        Plant plant = plantRepository.findByPlantIdAndDeletedAtIsNull(request.plantId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham"));
        LocalDateTime now = LocalDateTime.now();

        ProductReviews review = new ProductReviews();
        review.setProductReviewsId(UUID.randomUUID().toString());
        review.setOrders(order);
        review.setPlant(plant);
        review.setUserEntity(user);
        review.setRating(request.rating());
        review.setTitle(trimToLength(request.title(), 50));
        review.setComment(trimToNull(request.comment()));
        applyImage(review, imageStorageService.read(imageFile));
        review.setIsApproved(true);
        review.setHelpfulCount(0);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewRepository.save(review);

        notificationService.sendToUser(
                user,
                3,
                "Da gui danh gia",
                "Cam on ban da danh gia " + safePlantTitle(plant) + ".",
                "{\"reviewId\":\"" + review.getProductReviewsId() + "\",\"plantId\":\"" + plant.getPlantId() + "\"}"
        );
        return catalogService.toReviewResponse(review);
    }

    @Transactional
    @Override
    public ReviewResponse updateReview(UserEntity user, String reviewId, ReviewUpdateRequest request) {
        return updateReview(user, reviewId, request, null);
    }

    @Transactional
    @Override
    public ReviewResponse updateReview(UserEntity user, String reviewId, ReviewUpdateRequest request, MultipartFile imageFile) {
        validateReviewFields(request == null ? null : request.rating(), request == null ? null : request.comment());
        ProductReviews review = findMyReview(user, reviewId);
        review.setRating(request.rating());
        review.setTitle(trimToLength(request.title(), 50));
        review.setComment(trimToNull(request.comment()));
        applyImage(review, imageStorageService.read(imageFile));
        review.setUpdatedAt(LocalDateTime.now());
        return catalogService.toReviewResponse(review);
    }

    @Transactional
    @Override
    public void deleteReview(UserEntity user, String reviewId) {
        ProductReviews review = findMyReview(user, reviewId);
        reviewRepository.delete(review);
    }

    private ProductReviews findMyReview(UserEntity user, String reviewId) {
        if (reviewId == null || reviewId.isBlank()) {
            throw new IllegalArgumentException("Khong tim thay danh gia");
        }
        return reviewRepository.findByProductReviewsIdAndUserEntityUserId(reviewId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh gia"));
    }

    private boolean orderContainsPlant(Orders order, String plantId) {
        for (OrderItems item : order.getOrderItemsList()) {
            if (item.getProductVariant() != null
                    && item.getProductVariant().getPlant() != null
                    && plantId.equals(item.getProductVariant().getPlant().getPlantId())) {
                return true;
            }
        }
        return false;
    }

    private void validateReviewFields(Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("So sao danh gia phai tu 1 den 5");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Noi dung danh gia khong duoc de trong");
        }
    }

    private String safePlantTitle(Plant plant) {
        return plant == null || plant.getTitle() == null ? "san pham" : plant.getTitle();
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void applyImage(ProductReviews review, StoredImage image) {
        if (image == null) {
            return;
        }
        review.setImagesData(image.data());
        review.setImagesContentType(image.contentType());
        review.setImagesFileName(image.fileName());
        review.setImagesSize(image.size());
    }
}
