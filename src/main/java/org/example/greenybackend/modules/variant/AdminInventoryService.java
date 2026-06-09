package org.example.greenybackend.modules.variant;

import java.util.List;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;

public interface AdminInventoryService {

    List<ProductVariantResponse> getInventoryItems();

    List<ProductVariantResponse> getInventoryItems(
            String plant,
            String sku,
            String categoryId,
            String status,
            Integer minQuantity,
            Integer maxQuantity
    );

    ProductVariantResponse updateStock(String variantId, String movementType, Integer amount);

    ProductVariantResponse setTracking(String variantId, boolean isActive);

}
