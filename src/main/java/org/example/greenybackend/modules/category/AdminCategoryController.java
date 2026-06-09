package org.example.greenybackend.modules.category;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.category.dto.CategoryRequest;
import org.example.greenybackend.modules.category.dto.CategoryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final AdminCatalogService catalogService;

    public AdminCategoryController(AdminCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        return catalogService.getAllCategories();
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse getById(@PathVariable String categoryId) {
        return catalogService.getCategory(categoryId);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request));
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(
            @PathVariable String categoryId,
            @RequestBody CategoryRequest request
    ) {
        return catalogService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public MessageResponse deactivate(@PathVariable String categoryId) {
        catalogService.deactivateCategory(categoryId);
        return new MessageResponse("Da vo hieu hoa danh muc.");
    }
}
