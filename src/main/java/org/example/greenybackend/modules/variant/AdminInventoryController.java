package org.example.greenybackend.modules.variant;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final AdminInventoryService inventoryService;

    public AdminInventoryController(AdminInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<ProductVariantResponse> getAll(
            @RequestParam(required = false) String plant,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity
    ) {
        return inventoryService.getInventoryItems(plant, sku, categoryId, status, minQuantity, maxQuantity);
    }

    @PostMapping("/{variantId}/stock")
    public ProductVariantResponse updateStock(
            @PathVariable String variantId,
            @RequestParam String movementType,
            @RequestParam Integer amount
    ) {
        return inventoryService.updateStock(variantId, movementType, amount);
    }

    @PatchMapping("/{variantId}/visibility")
    public ProductVariantResponse setTracking(
            @PathVariable String variantId,
            @RequestParam Boolean isActive
    ) {
        return inventoryService.setTracking(variantId, Boolean.TRUE.equals(isActive));
    }

    @PostMapping("/{variantId}/deactivate")
    public MessageResponse stopTracking(@PathVariable String variantId) {
        inventoryService.setTracking(variantId, false);
        return new MessageResponse("Da ngung theo doi ton kho.");
    }
}
