package org.example.greenybackend.modules.category;

import java.util.List;
import org.example.greenybackend.modules.category.dto.CategoryRequest;
import org.example.greenybackend.modules.category.dto.CategoryResponse;

public interface AdminCatalogService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategory(String categoryId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(String categoryId, CategoryRequest request);

    void setCategoryActive(String categoryId, boolean isActive);

    void deactivateCategory(String categoryId);

    void deleteCategoryIfUnused(String categoryId);

}
