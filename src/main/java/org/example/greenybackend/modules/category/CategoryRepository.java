package org.example.greenybackend.modules.category;

import java.util.List;
import org.example.greenybackend.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findByIsActiveTrueOrderBySortOrderAscTitleAsc();
}
