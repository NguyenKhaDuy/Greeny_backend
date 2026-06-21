package org.example.greenybackend.modules.category;

import java.util.List;
import org.example.greenybackend.modules.category.dto.CategoryRequest;
import org.example.greenybackend.modules.category.dto.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AdminCatalogService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategory(String categoryId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile);

    CategoryResponse updateCategory(String categoryId, CategoryRequest request);

    CategoryResponse updateCategory(String categoryId, CategoryRequest request, MultipartFile imageFile);

    void setCategoryActive(String categoryId, boolean isActive);

    void deactivateCategory(String categoryId);

    void deleteCategoryIfUnused(String categoryId);

}
