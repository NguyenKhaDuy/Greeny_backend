package org.example.greenybackend.modules.variant.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.isBlankOrAll;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Category;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.example.greenybackend.modules.variant.AdminVariantService;
import org.example.greenybackend.modules.variant.ProductVariantRepository;
import org.example.greenybackend.modules.variant.dto.ProductVariantRequest;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminVariantServiceImpl implements AdminVariantService {

    private final ProductVariantRepository variantRepository;
    private final PlantRepository plantRepository;

    public AdminVariantServiceImpl(ProductVariantRepository variantRepository, PlantRepository plantRepository) {
        this.variantRepository = variantRepository;
        this.plantRepository = plantRepository;
    }

    @Override
    public List<ProductVariantResponse> getAllVariants(String plantId) {
        return getAllVariants(plantId, null, null, null, null, null, null, null, null);
    }

    @Override
    public List<ProductVariantResponse> getAllVariants(
            String plantId,
            String name,
            String sku,
            String categoryId,
            String categoryTitle,
            String stock,
            String status,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        List<ProductVariant> variants = isBlankOrAll(plantId)
                ? variantRepository.findAll()
                : variantRepository.findByPlantPlantId(plantId);
        return variants.stream()
                .filter(variant -> contains(variant.getName(), name))
                .filter(variant -> contains(variant.getSku(), sku))
                .filter(variant -> matchesCategoryId(variant, categoryId))
                .filter(variant -> matchesCategoryTitle(variant, categoryTitle))
                .filter(variant -> matchesStock(variant, stock))
                .filter(variant -> matchesStatus(variant, status))
                .filter(variant -> matchesPrice(variant, minPrice, maxPrice))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductVariantResponse getVariant(String variantId) {
        return toResponse(findVariant(variantId));
    }

    @Transactional
    @Override
    public ProductVariantResponse createVariant(ProductVariantRequest request) {
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(UUID.randomUUID().toString());
        applyRequest(variant, request);
        variant.setIsActive(request.isActive() == null || request.isActive());
        variant.setCreatedAt(now);
        variant.setUpdatedAt(now);
        return toResponse(variantRepository.save(variant));
    }

    @Transactional
    @Override
    public ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request) {
        validateRequest(request);

        ProductVariant variant = findVariant(variantId);
        applyRequest(variant, request);
        variant.setUpdatedAt(LocalDateTime.now());
        return toResponse(variant);
    }

    @Transactional
    @Override
    public void setVariantActive(String variantId, boolean isActive) {
        ProductVariant variant = findVariant(variantId);
        variant.setIsActive(isActive);
        variant.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deactivateVariant(String variantId) {
        setVariantActive(variantId, false);
    }

    private ProductVariant findVariant(String variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể"));
    }

    private boolean matchesCategoryId(ProductVariant variant, String categoryId) {
        if (isBlankOrAll(categoryId)) {
            return true;
        }
        Category category = categoryOf(variant);
        String normalizedCategoryId = categoryId.trim();
        if ("none".equalsIgnoreCase(normalizedCategoryId)) {
            return category == null || category.getCaId() == null;
        }
        return category != null && normalizedCategoryId.equals(category.getCaId());
    }

    private boolean matchesCategoryTitle(ProductVariant variant, String categoryTitle) {
        if (isBlankOrAll(categoryTitle)) {
            return true;
        }
        Category category = categoryOf(variant);
        if ("none".equalsIgnoreCase(categoryTitle.trim())) {
            return category == null || category.getTitle() == null || category.getTitle().isBlank();
        }
        return contains(category == null ? null : category.getTitle(), categoryTitle);
    }

    private boolean matchesStock(ProductVariant variant, String stock) {
        if (isBlankOrAll(stock)) {
            return true;
        }
        String normalizedStock = stock.trim().toLowerCase();
        int quantity = variant.getQuantity() == null ? 0 : variant.getQuantity();
        return switch (normalizedStock) {
            case "out" -> quantity <= 0;
            case "low" -> quantity > 0 && quantity <= 5;
            case "ok", "in-stock" -> quantity > 5;
            default -> true;
        };
    }

    private boolean matchesStatus(ProductVariant variant, String status) {
        if (isBlankOrAll(status)) {
            return true;
        }
        String normalizedStatus = status.trim().toLowerCase();
        boolean active = Boolean.TRUE.equals(variant.getIsActive());
        return switch (normalizedStatus) {
            case "active", "visible", "true" -> active;
            case "inactive", "hidden", "stopped", "false" -> !active;
            default -> true;
        };
    }

    private boolean matchesPrice(ProductVariant variant, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal price = variant.getSalePrice() != null ? variant.getSalePrice() : variant.getPrice();
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        return (minPrice == null || price.compareTo(minPrice) >= 0)
                && (maxPrice == null || price.compareTo(maxPrice) <= 0);
    }

    private Category categoryOf(ProductVariant variant) {
        Plant plant = variant.getPlant();
        return plant == null ? null : plant.getCategory();
    }

    private void applyRequest(ProductVariant variant, ProductVariantRequest request) {
        variant.setPlant(findPlant(request.plantId()));
        variant.setName(trimToNull(request.name()));
        variant.setSku(trimToNull(request.sku()));
        variant.setHeightCm(request.heightCm());
        variant.setPotSize(request.potSize());
        variant.setPrice(request.price());
        variant.setSalePrice(request.salePrice());
        variant.setQuantity(request.quantity());
        variant.setAttribute(trimToNull(request.attribute()));
        if (request.isActive() != null) {
            variant.setIsActive(request.isActive());
        }
        variant.setSeoDescription(trimToNull(request.seoDescription()));
        variant.setSeoTitle(trimToNull(request.seoTitle()));
    }

    private Plant findPlant(String plantId) {
        if (plantId == null || plantId.isBlank()) {
            throw new IllegalArgumentException("Biến thể phải thuộc về một cây");
        }
        return plantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cây"));
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        Plant plant = variant.getPlant();
        return new ProductVariantResponse(
                variant.getVariantId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                variant.getName(),
                variant.getSku(),
                variant.getHeightCm(),
                variant.getPotSize(),
                variant.getPrice(),
                variant.getSalePrice(),
                variant.getQuantity(),
                variant.getAttribute(),
                variant.getIsActive(),
                variant.getSeoDescription(),
                variant.getSeoTitle(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }

    private void validateRequest(ProductVariantRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Tên biến thể không được để trống");
        }
        if (request.price() == null) {
            throw new IllegalArgumentException("Giá biến thể không được để trống");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
