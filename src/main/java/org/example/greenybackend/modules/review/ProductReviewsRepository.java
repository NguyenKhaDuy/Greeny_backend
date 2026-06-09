package org.example.greenybackend.modules.review;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewsRepository extends JpaRepository<ProductReviews, String> {

    @Override
    @EntityGraph(attributePaths = {"orders", "plant", "plant.category", "userEntity"})
    List<ProductReviews> findAll();

    @Override
    @EntityGraph(attributePaths = {"orders", "plant", "plant.category", "userEntity"})
    Optional<ProductReviews> findById(String reviewId);

    List<ProductReviews> findByPlantPlantIdAndIsApprovedTrueOrderByCreatedAtDesc(String plantId);

    List<ProductReviews> findByUserEntityUserIdOrderByCreatedAtDesc(String userId);

    Optional<ProductReviews> findByProductReviewsIdAndUserEntityUserId(String reviewId, String userId);

    boolean existsByUserEntityUserIdAndOrdersOrderIdAndPlantPlantId(String userId, String orderId, String plantId);
}
