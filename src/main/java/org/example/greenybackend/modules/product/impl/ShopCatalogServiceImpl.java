package org.example.greenybackend.modules.product.impl;

import org.example.greenybackend.modules.product.ShopCatalogService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.example.greenybackend.common.response.PageResponse;
import org.example.greenybackend.domain.entity.Category;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductImage;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.category.CategoryRepository;
import org.example.greenybackend.modules.category.dto.ShopCategoryResponse;
import org.example.greenybackend.modules.order.OrderItemsRepository;
import org.example.greenybackend.modules.plant.dto.ShopPlantDetailResponse;
import org.example.greenybackend.modules.plant.dto.ShopPlantSummaryResponse;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.example.greenybackend.modules.review.ProductReviewsRepository;
import org.example.greenybackend.modules.variant.dto.ShopVariantResponse;
import org.example.greenybackend.modules.variant.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopCatalogServiceImpl implements ShopCatalogService {

    private final CategoryRepository categoryRepository;
    private final PlantRepository plantRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductReviewsRepository reviewRepository;
    private final OrderItemsRepository orderItemsRepository;

    public ShopCatalogServiceImpl(
            CategoryRepository categoryRepository,
            PlantRepository plantRepository,
            ProductVariantRepository variantRepository,
            ProductReviewsRepository reviewRepository,
            OrderItemsRepository orderItemsRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.plantRepository = plantRepository;
        this.variantRepository = variantRepository;
        this.reviewRepository = reviewRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ShopCategoryResponse> getCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAscTitleAsc().stream()
                .map(category -> new ShopCategoryResponse(
                        category.getCaId(),
                        category.getTitle(),
                        category.getDescription(),
                        category.getImageUrl(),
                        category.getSortOrder()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ShopPlantSummaryResponse> searchPlants(
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
    ) {
        int safePage = Math.max(page == null ? 0 : page, 0);
        int safeSize = Math.min(Math.max(size == null ? 12 : size, 1), 60);
        String normalizedQuery = normalize(query);
        String normalizedAttribute = normalize(variantAttribute);

        List<ProductVariant> variants = variantRepository.findByIsActiveTrue().stream()
                .filter(variant -> variant.getPlant() != null)
                .filter(variant -> variant.getPlant().getDeletedAt() == null)
                .filter(variant -> matchesQuery(variant, normalizedQuery))
                .filter(variant -> matchesCategory(variant, categoryId))
                .filter(variant -> matchesPlantType(variant, plantType))
                .filter(variant -> matchesAttribute(variant, normalizedAttribute))
                .filter(variant -> matchesStock(variant, inStock))
                .filter(variant -> matchesPrice(variant, minPrice, maxPrice))
                .toList();

        Map<String, List<ProductVariant>> variantsByPlant = variants.stream()
                .collect(Collectors.groupingBy(
                        variant -> variant.getPlant().getPlantId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<String, Long> soldCounts = buildSoldCountByPlant();

        List<ShopPlantSummaryResponse> summaries = variantsByPlant.values().stream()
                .map(this::toPlantSummary)
                .sorted(comparatorFor(sort, soldCounts))
                .toList();

        int from = Math.min(safePage * safeSize, summaries.size());
        int to = Math.min(from + safeSize, summaries.size());
        List<ShopPlantSummaryResponse> pageItems = summaries.subList(from, to);
        int totalPages = summaries.isEmpty() ? 0 : (int) Math.ceil((double) summaries.size() / safeSize);
        return new PageResponse<>(
                pageItems,
                safePage,
                safeSize,
                summaries.size(),
                totalPages,
                safePage == 0,
                safePage >= Math.max(totalPages - 1, 0)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ShopPlantDetailResponse getPlantDetail(String plantId) {
        Plant plant = plantRepository.findByPlantIdAndDeletedAtIsNull(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham"));

        List<ProductVariant> variants = plant.getProductVariants().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .toList();
        List<ShopVariantResponse> variantResponses = variants.stream().map(this::toVariantResponse).toList();
        List<ReviewResponse> reviews = getPlantReviews(plantId);
        BigDecimal minPrice = variants.stream().map(this::effectivePrice).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = variants.stream().map(this::effectivePrice).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<String> images = variants.stream()
                .flatMap(variant -> safeImages(variant).stream())
                .distinct()
                .toList();

        Category category = plant.getCategory();
        return new ShopPlantDetailResponse(
                plant.getPlantId(),
                plant.getTitle(),
                plant.getSku(),
                plant.getDescription(),
                plant.getScientificName(),
                plant.getCommonName(),
                plant.getPlantType(),
                plant.getOrigin(),
                plant.getToxicity(),
                plant.getPetFriendly(),
                plant.getAirPurifying(),
                category == null ? null : category.getCaId(),
                category == null ? null : category.getTitle(),
                minPrice,
                maxPrice,
                totalStock(variants),
                averageRating(reviews),
                (long) reviews.size(),
                firstImage(variants),
                images,
                variantResponses,
                reviews,
                plant.getCreatedAt(),
                plant.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getPlantReviews(String plantId) {
        return reviewRepository.findByPlantPlantIdAndIsApprovedTrueOrderByCreatedAtDesc(plantId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    public ShopVariantResponse toVariantResponse(ProductVariant variant) {
        Plant plant = variant.getPlant();
        List<String> images = safeImages(variant);
        return new ShopVariantResponse(
                variant.getVariantId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                variant.getName(),
                variant.getSku(),
                variant.getHeightCm(),
                variant.getPotSize(),
                variant.getPrice(),
                variant.getSalePrice(),
                effectivePrice(variant),
                stock(variant),
                variant.getAttribute(),
                variant.getIsActive(),
                images.isEmpty() ? null : images.get(0),
                images
        );
    }

    @Override
    public ReviewResponse toReviewResponse(ProductReviews review) {
        Plant plant = review.getPlant();
        UserEntity user = review.getUserEntity();
        return new ReviewResponse(
                review.getProductReviewsId(),
                review.getOrders() == null ? null : review.getOrders().getOrderId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                user == null ? null : user.getUserId(),
                user == null ? null : user.getTitle(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getImages(),
                review.getIsApproved(),
                review.getHelpfulCount(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    @Override
    public BigDecimal effectivePrice(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        if (variant.getSalePrice() != null && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getSalePrice();
        }
        return variant.getPrice() == null ? BigDecimal.ZERO : variant.getPrice();
    }

    @Override
    public String firstImage(ProductVariant variant) {
        List<String> images = safeImages(variant);
        return images.isEmpty() ? null : images.get(0);
    }

    private ShopPlantSummaryResponse toPlantSummary(List<ProductVariant> variants) {
        Plant plant = variants.get(0).getPlant();
        Category category = plant.getCategory();
        List<ReviewResponse> reviews = getPlantReviews(plant.getPlantId());
        BigDecimal minPrice = variants.stream().map(this::effectivePrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = variants.stream().map(this::effectivePrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        return new ShopPlantSummaryResponse(
                plant.getPlantId(),
                plant.getTitle(),
                plant.getDescription(),
                category == null ? null : category.getCaId(),
                category == null ? null : category.getTitle(),
                plant.getPlantType(),
                minPrice,
                maxPrice,
                totalStock(variants),
                averageRating(reviews),
                (long) reviews.size(),
                firstImage(variants),
                plant.getCreatedAt()
        );
    }

    private Comparator<ShopPlantSummaryResponse> comparatorFor(String sort, Map<String, Long> soldCounts) {
        String normalizedSort = sort == null ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSort) {
            case "price_asc" -> Comparator.comparing(ShopPlantSummaryResponse::minPrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "price_desc" -> Comparator.comparing(ShopPlantSummaryResponse::maxPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
            case "bestselling" -> Comparator
                    .comparing((ShopPlantSummaryResponse item) -> soldCounts.getOrDefault(item.plantId(), 0L))
                    .reversed()
                    .thenComparing(ShopPlantSummaryResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(ShopPlantSummaryResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private Map<String, Long> buildSoldCountByPlant() {
        return orderItemsRepository.findAll().stream()
                .filter(item -> item.getOrders() == null || item.getOrders().getStatus() == null || item.getOrders().getStatus() != 5)
                .filter(item -> item.getProductVariant() != null && item.getProductVariant().getPlant() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProductVariant().getPlant().getPlantId(),
                        Collectors.summingLong(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                ));
    }

    private boolean matchesQuery(ProductVariant variant, String query) {
        if (query == null) {
            return true;
        }
        Plant plant = variant.getPlant();
        Category category = plant.getCategory();
        return contains(plant.getTitle(), query)
                || contains(plant.getDescription(), query)
                || contains(plant.getScientificName(), query)
                || contains(plant.getCommonName(), query)
                || contains(variant.getName(), query)
                || contains(variant.getAttribute(), query)
                || (category != null && contains(category.getTitle(), query));
    }

    private boolean matchesCategory(ProductVariant variant, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return true;
        }
        Category category = variant.getPlant().getCategory();
        return category != null && categoryId.equals(category.getCaId());
    }

    private boolean matchesPlantType(ProductVariant variant, Integer plantType) {
        return plantType == null || plantType.equals(variant.getPlant().getPlantType());
    }

    private boolean matchesAttribute(ProductVariant variant, String attribute) {
        return attribute == null || contains(variant.getAttribute(), attribute) || contains(variant.getName(), attribute);
    }

    private boolean matchesStock(ProductVariant variant, Boolean inStock) {
        return inStock == null || !inStock || stock(variant) > 0;
    }

    private boolean matchesPrice(ProductVariant variant, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal price = effectivePrice(variant);
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        return maxPrice == null || price.compareTo(maxPrice) <= 0;
    }

    private boolean contains(String value, String query) {
        String normalized = normalize(value);
        return normalized != null && normalized.contains(query);
    }

    private int totalStock(List<ProductVariant> variants) {
        return variants.stream().mapToInt(this::stock).sum();
    }

    private int stock(ProductVariant variant) {
        return variant.getQuantity() == null ? 0 : Math.max(variant.getQuantity(), 0);
    }

    private Double averageRating(List<ReviewResponse> reviews) {
        return reviews.stream()
                .map(ReviewResponse::rating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .map(value -> Math.round(value * 10.0) / 10.0)
                .boxed()
                .findFirst()
                .orElse(0.0);
    }

    private String firstImage(List<ProductVariant> variants) {
        return variants.stream()
                .map(this::firstImage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<String> safeImages(ProductVariant variant) {
        if (variant == null || variant.getProductImages() == null) {
            return List.of();
        }
        List<String> images = new ArrayList<>();
        for (ProductImage image : variant.getProductImages()) {
            if (image.getImageUrl() != null && !image.getImageUrl().isBlank()) {
                images.add(image.getImageUrl());
            }
        }
        return images;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
