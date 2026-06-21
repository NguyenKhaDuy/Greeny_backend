package org.example.greenybackend.modules.plant;

import java.util.List;
import java.time.LocalDate;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleRequest;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AdminArticleService {

    List<PlantCareArticleResponse> getAllArticles();

    List<PlantCareArticleResponse> getAllArticles(String title, String slug, String status, LocalDate created);

    PlantCareArticleResponse getArticle(String articleId);

    PlantCareArticleResponse createArticle(PlantCareArticleRequest request, UserEntity author);

    PlantCareArticleResponse createArticle(PlantCareArticleRequest request, UserEntity author, MultipartFile thumbnailFile);

    PlantCareArticleResponse updateArticle(String articleId, PlantCareArticleRequest request);

    PlantCareArticleResponse updateArticle(String articleId, PlantCareArticleRequest request, MultipartFile thumbnailFile);

    void deleteArticle(String articleId);

}
