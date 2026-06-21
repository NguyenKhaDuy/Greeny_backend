package org.example.greenybackend.modules.category;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.category.dto.CategoryRequest;
import org.example.greenybackend.modules.category.dto.CategoryResponse;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> create(@RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryResponse> createMultipart(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) MultipartFile imageFile
    ) {
        CategoryRequest request = new CategoryRequest(title, description, isActive, sortOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request, imageFile));
    }

    @PutMapping(value = "/{categoryId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CategoryResponse update(
            @PathVariable String categoryId,
            @RequestBody CategoryRequest request
    ) {
        return catalogService.updateCategory(categoryId, request);
    }

    @PutMapping(value = "/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoryResponse updateMultipart(
            @PathVariable String categoryId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) MultipartFile imageFile
    ) {
        return catalogService.updateCategory(categoryId, new CategoryRequest(title, description, isActive, sortOrder), imageFile);
    }

    @PatchMapping("/{categoryId}/visibility")
    public MessageResponse setVisibility(
            @PathVariable String categoryId,
            @RequestParam Boolean isActive
    ) {
        catalogService.setCategoryActive(categoryId, Boolean.TRUE.equals(isActive));
        return new MessageResponse(Boolean.TRUE.equals(isActive) ? "Da hien thi danh muc." : "Da an danh muc.");
    }

    @DeleteMapping("/{categoryId}")
    public MessageResponse deactivate(@PathVariable String categoryId) {
        catalogService.deactivateCategory(categoryId);
        return new MessageResponse("Da vo hieu hoa danh muc.");
    }
}
