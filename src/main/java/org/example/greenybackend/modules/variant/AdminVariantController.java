package org.example.greenybackend.modules.variant;

import java.math.BigDecimal;
import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.variant.dto.ProductVariantRequest;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductVariantResponse> create(@RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.createVariant(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductVariantResponse> createMultipart(
            @RequestParam String plantId,
            @RequestParam String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer heightCm,
            @RequestParam(required = false) Integer potSize,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal salePrice,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) String seoDescription,
            @RequestParam(required = false) String seoTitle,
            @RequestParam(required = false) MultipartFile[] imageFiles
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.createVariant(
                request(plantId, name, sku, heightCm, potSize, price, salePrice, quantity, attribute, isActive, seoDescription, seoTitle),
                imageFiles
        ));
    }

    @PutMapping(value = "/{variantId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductVariantResponse update(
            @PathVariable String variantId,
            @RequestBody ProductVariantRequest request
    ) {
        return variantService.updateVariant(variantId, request);
    }

    @PutMapping(value = "/{variantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductVariantResponse updateMultipart(
            @PathVariable String variantId,
            @RequestParam String plantId,
            @RequestParam String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer heightCm,
            @RequestParam(required = false) Integer potSize,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal salePrice,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) String seoDescription,
            @RequestParam(required = false) String seoTitle,
            @RequestParam(required = false) MultipartFile[] imageFiles
    ) {
        return variantService.updateVariant(
                variantId,
                request(plantId, name, sku, heightCm, potSize, price, salePrice, quantity, attribute, isActive, seoDescription, seoTitle),
                imageFiles
        );
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

    private ProductVariantRequest request(
            String plantId,
            String name,
            String sku,
            Integer heightCm,
            Integer potSize,
            BigDecimal price,
            BigDecimal salePrice,
            Integer quantity,
            String attribute,
            Boolean isActive,
            String seoDescription,
            String seoTitle
    ) {
        return new ProductVariantRequest(
                plantId,
                name,
                sku,
                heightCm,
                potSize,
                price,
                salePrice,
                quantity,
                attribute,
                isActive,
                seoDescription,
                seoTitle
        );
    }
}
