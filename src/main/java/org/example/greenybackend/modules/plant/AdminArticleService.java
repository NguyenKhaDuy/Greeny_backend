package org.example.greenybackend.modules.plant;

import java.util.List;
import java.time.LocalDate;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleRequest;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleResponse;

public interface AdminArticleService {

    List<PlantCareArticleResponse> getAllArticles();

    List<PlantCareArticleResponse> getAllArticles(String title, String slug, String status, LocalDate created);

    PlantCareArticleResponse getArticle(String articleId);

    PlantCareArticleResponse createArticle(PlantCareArticleRequest request, UserEntity author);

    PlantCareArticleResponse updateArticle(String articleId, PlantCareArticleRequest request);

    void deleteArticle(String articleId);

}
