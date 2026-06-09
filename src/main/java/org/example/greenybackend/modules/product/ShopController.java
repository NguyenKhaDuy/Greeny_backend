package org.example.greenybackend.modules.product;

import java.math.BigDecimal;
import java.util.List;
import org.example.greenybackend.common.response.PageResponse;
import org.example.greenybackend.modules.category.dto.ShopCategoryResponse;
import org.example.greenybackend.modules.plant.dto.ShopPlantDetailResponse;
import org.example.greenybackend.modules.plant.dto.ShopPlantSummaryResponse;
import org.example.greenybackend.modules.review.dto.ReviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopCatalogService catalogService;

    public ShopController(ShopCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public List<ShopCategoryResponse> categories() {
        return catalogService.getCategories();
    }

    @GetMapping("/plants")
    public PageResponse<ShopPlantSummaryResponse> plants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Integer plantType,
            @RequestParam(required = false) String variantAttribute,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size
    ) {
        return catalogService.searchPlants(query, categoryId, minPrice, maxPrice, inStock, plantType, variantAttribute, sort, page, size);
    }

    @GetMapping("/plants/{plantId}")
    public ShopPlantDetailResponse plantDetail(@PathVariable String plantId) {
        return catalogService.getPlantDetail(plantId);
    }

    @GetMapping("/plants/{plantId}/reviews")
    public List<ReviewResponse> plantReviews(@PathVariable String plantId) {
        return catalogService.getPlantReviews(plantId);
    }
}
