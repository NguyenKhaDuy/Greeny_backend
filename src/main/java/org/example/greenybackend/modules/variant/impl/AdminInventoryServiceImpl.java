package org.example.greenybackend.modules.variant.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.isBlankOrAll;

import java.time.LocalDateTime;
import java.util.List;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.modules.variant.AdminInventoryService;
import org.example.greenybackend.modules.variant.ProductVariantRepository;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminInventoryServiceImpl implements AdminInventoryService {

    public static final String TYPE_IMPORT = "IMPORT";
    public static final String TYPE_EXPORT = "EXPORT";
    public static final String TYPE_ADJUST = "ADJUST";

    private final ProductVariantRepository variantRepository;

    public AdminInventoryServiceImpl(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Override
    public List<ProductVariantResponse> getInventoryItems() {
        return getInventoryItems(null, null, null, null, null, null);
    }

    @Override
    public List<ProductVariantResponse> getInventoryItems(
            String plant,
            String sku,
            String categoryId,
            String status,
            Integer minQuantity,
            Integer maxQuantity
    ) {
        return variantRepository.findAll().stream()
                .filter(variant -> matchesPlant(variant, plant))
                .filter(variant -> contains(variant.getSku(), sku))
                .filter(variant -> matchesCategory(variant, categoryId))
                .filter(variant -> matchesStatus(variant, status))
                .filter(variant -> matchesQuantityRange(variant, minQuantity, maxQuantity))
                .map(this::toVariantResponse)
                .toList();
    }

    @Transactional
    @Override
    public ProductVariantResponse updateStock(String variantId, String movementType, Integer amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("So luong phai lon hon hoac bang 0");
        }
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bien the"));

        int before = variant.getQuantity() == null ? 0 : variant.getQuantity();
        int change = calculateChange(movementType, amount, before);
        int after = before + change;
        if (after < 0) {
            throw new IllegalArgumentException("Ton kho khong duoc am");
        }

        LocalDateTime now = LocalDateTime.now();
        variant.setQuantity(after);
        variant.setUpdatedAt(now);
        return toVariantResponse(variantRepository.save(variant));
    }

    @Transactional
    @Override
    public ProductVariantResponse setTracking(String variantId, boolean isActive) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bien the"));
        variant.setIsActive(isActive);
        variant.setUpdatedAt(LocalDateTime.now());
        return toVariantResponse(variant);
    }

    private boolean matchesPlant(ProductVariant variant, String plant) {
        Plant parent = variant.getPlant();
        String plantText = parent == null ? "" : parent.getTitle();
        return contains(plantText, plant);
    }

    private boolean matchesCategory(ProductVariant variant, String categoryId) {
        if (isBlankOrAll(categoryId)) {
            return true;
        }
        Plant parent = variant.getPlant();
        String normalizedCategory = categoryId.trim();
        if ("none".equalsIgnoreCase(normalizedCategory)) {
            return parent == null || parent.getCategory() == null || parent.getCategory().getCaId() == null;
        }
        return parent != null
                && parent.getCategory() != null
                && normalizedCategory.equals(parent.getCategory().getCaId());
    }

    private boolean matchesStatus(ProductVariant variant, String status) {
        if (isBlankOrAll(status)) {
            return true;
        }
        String currentStatus = inventoryStatus(variant);
        String normalizedStatus = status.trim().toLowerCase();
        if ("active".equals(normalizedStatus)) {
            return Boolean.TRUE.equals(variant.getIsActive());
        }
        if ("inactive".equals(normalizedStatus) || "hidden".equals(normalizedStatus)) {
            return !Boolean.TRUE.equals(variant.getIsActive());
        }
        return currentStatus.equals(normalizedStatus);
    }

    private boolean matchesQuantityRange(ProductVariant variant, Integer minQuantity, Integer maxQuantity) {
        int quantity = quantityOf(variant);
        return (minQuantity == null || quantity >= minQuantity)
                && (maxQuantity == null || quantity <= maxQuantity);
    }

    private String inventoryStatus(ProductVariant variant) {
        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            return "stopped";
        }
        int quantity = quantityOf(variant);
        if (quantity <= 0) {
            return "out";
        }
        if (quantity <= 5) {
            return "low";
        }
        return "ok";
    }

    private int quantityOf(ProductVariant variant) {
        return variant.getQuantity() == null ? 0 : variant.getQuantity();
    }

    private int calculateChange(String movementType, int amount, int before) {
        String normalizedType = normalizeType(movementType);
        return switch (normalizedType) {
            case TYPE_IMPORT -> amount;
            case TYPE_EXPORT -> -amount;
            case TYPE_ADJUST -> amount - before;
            default -> throw new IllegalArgumentException("Loai bien dong kho khong hop le");
        };
    }

    private String normalizeType(String movementType) {
        if (movementType == null || movementType.isBlank()) {
            throw new IllegalArgumentException("Loai bien dong kho khong hop le");
        }
        String normalizedType = movementType.trim().toUpperCase();
        if (!TYPE_IMPORT.equals(normalizedType) && !TYPE_EXPORT.equals(normalizedType) && !TYPE_ADJUST.equals(normalizedType)) {
            throw new IllegalArgumentException("Loai bien dong kho khong hop le");
        }
        return normalizedType;
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant) {
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

}
