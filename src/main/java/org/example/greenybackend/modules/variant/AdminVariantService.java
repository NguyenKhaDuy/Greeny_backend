package org.example.greenybackend.modules.variant;

import java.util.List;
import org.example.greenybackend.modules.variant.dto.ProductVariantRequest;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AdminVariantService {

    List<ProductVariantResponse> getAllVariants(String plantId);

    List<ProductVariantResponse> getAllVariants(
            String plantId,
            String name,
            String sku,
            String categoryId,
            String categoryTitle,
            String stock,
            String status,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice
    );

    ProductVariantResponse getVariant(String variantId);

    ProductVariantResponse createVariant(ProductVariantRequest request);

    ProductVariantResponse createVariant(ProductVariantRequest request, MultipartFile[] imageFiles);

    ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request, MultipartFile[] imageFiles);

    void setVariantActive(String variantId, boolean isActive);

    void deactivateVariant(String variantId);

}
