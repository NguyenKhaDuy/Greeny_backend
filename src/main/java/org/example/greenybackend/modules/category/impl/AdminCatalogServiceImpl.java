package org.example.greenybackend.modules.category.impl;

import org.example.greenybackend.modules.category.AdminCatalogService;
import org.example.greenybackend.modules.category.CategoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Category;
import org.example.greenybackend.modules.category.dto.CategoryRequest;
import org.example.greenybackend.modules.category.dto.CategoryResponse;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final CategoryRepository categoryRepository;
    private final PlantRepository plantRepository;

    public AdminCatalogServiceImpl(CategoryRepository categoryRepository, PlantRepository plantRepository) {
        this.categoryRepository = categoryRepository;
        this.plantRepository = plantRepository;
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategory(String categoryId) {
        return toResponse(findCategory(categoryId));
    }

    @Transactional
    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        validateTitle(request.title());

        LocalDateTime now = LocalDateTime.now();
        Category category = new Category();
        category.setCaId(UUID.randomUUID().toString());
        applyRequest(category, request);
        category.setIsActive(request.isActive() == null || request.isActive());
        category.setCreatedCa(now);
        category.setUpdatedCa(now);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    @Override
    public CategoryResponse updateCategory(String categoryId, CategoryRequest request) {
        validateTitle(request.title());

        Category category = findCategory(categoryId);
        applyRequest(category, request);
        category.setUpdatedCa(LocalDateTime.now());
        return toResponse(category);
    }

    @Transactional
    @Override
    public void setCategoryActive(String categoryId, boolean isActive) {
        Category category = findCategory(categoryId);
        category.setIsActive(isActive);
        category.setUpdatedCa(LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deactivateCategory(String categoryId) {
        setCategoryActive(categoryId, false);
    }

    @Transactional
    @Override
    public void deleteCategoryIfUnused(String categoryId) {
        long linkedPlantCount = plantRepository.countByCategory_CaId(categoryId);
        if (linkedPlantCount > 0) {
            throw new IllegalArgumentException("Danh mục đang có sản phẩm, không thể xóa");
        }
        categoryRepository.delete(findCategory(categoryId));
    }

    private Category findCategory(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setTitle(trimToNull(request.title()));
        category.setDescription(trimToNull(request.description()));
        category.setImageUrl(trimToNull(request.imageUrl()));
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        category.setSortOrder(request.sortOrder());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getCaId(),
                category.getTitle(),
                category.getDescription(),
                category.getImageUrl(),
                category.getIsActive(),
                category.getSortOrder(),
                category.getCreatedCa(),
                category.getUpdatedCa()
        );
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
