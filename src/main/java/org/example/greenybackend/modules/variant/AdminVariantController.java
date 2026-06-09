package org.example.greenybackend.modules.variant;

import java.math.BigDecimal;
import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.variant.dto.ProductVariantRequest;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/variants")
public class AdminVariantController {

    private final AdminVariantService variantService;

    public AdminVariantController(AdminVariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping
    public List<ProductVariantResponse> getAll(
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String categoryTitle,
            @RequestParam(required = false) String stock,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return variantService.getAllVariants(plantId, name, sku, categoryId, categoryTitle, stock, status, minPrice, maxPrice);
    }

    @GetMapping("/{variantId}")
    public ProductVariantResponse getById(@PathVariable String variantId) {
        return variantService.getVariant(variantId);
    }

    @PostMapping
    public ResponseEntity<ProductVariantResponse> create(@RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.createVariant(request));
    }

    @PutMapping("/{variantId}")
    public ProductVariantResponse update(
            @PathVariable String variantId,
            @RequestBody ProductVariantRequest request
    ) {
        return variantService.updateVariant(variantId, request);
    }

    @PatchMapping("/{variantId}/visibility")
    public MessageResponse setVisibility(
            @PathVariable String variantId,
            @RequestParam Boolean isActive
    ) {
        variantService.setVariantActive(variantId, Boolean.TRUE.equals(isActive));
        return new MessageResponse(Boolean.TRUE.equals(isActive) ? "Da hien thi variant." : "Da an variant.");
    }

    @DeleteMapping("/{variantId}")
    public MessageResponse deactivate(@PathVariable String variantId) {
        variantService.deactivateVariant(variantId);
        return new MessageResponse("Da vo hieu hoa variant.");
    }
}
