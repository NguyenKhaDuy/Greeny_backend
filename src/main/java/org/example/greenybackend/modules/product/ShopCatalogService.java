package org.example.greenybackend.modules.product;

import java.math.BigDecimal;
import java.util.List;
import org.example.greenybackend.common.response.PageResponse;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.modules.category.dto.ShopCategoryResponse;
import org.example.greenybackend.modules.plant.dto.ShopPlantDetailResponse;
import org.example.greenybackend.modules.plant.dto.ShopPlantSummaryResponse;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.variant.dto.ShopVariantResponse;

public interface ShopCatalogService {

    List<ShopCategoryResponse> getCategories();

    PageResponse<ShopPlantSummaryResponse> searchPlants(
            String query,
            String categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            Integer plantType,
            String variantAttribute,
            String sort,
            Integer page,
            Integer size
    );

    ShopPlantDetailResponse getPlantDetail(String plantId);

    List<ReviewResponse> getPlantReviews(String plantId);

    ShopVariantResponse toVariantResponse(ProductVariant variant);

    ReviewResponse toReviewResponse(ProductReviews review);

    BigDecimal effectivePrice(ProductVariant variant);

    String firstImage(ProductVariant variant);

}
