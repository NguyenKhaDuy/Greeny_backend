package org.example.greenybackend.modules.plant;

import java.util.Optional;
import org.example.greenybackend.domain.entity.PlantCareArticles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantCareArticleRepository extends JpaRepository<PlantCareArticles, String> {

    Optional<PlantCareArticles> findBySlug(String slug);
}
