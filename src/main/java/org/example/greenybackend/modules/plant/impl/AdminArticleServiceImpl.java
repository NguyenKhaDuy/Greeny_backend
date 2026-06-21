package org.example.greenybackend.modules.plant.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.dateEquals;
import static org.example.greenybackend.common.util.AdminFilters.isBlankOrAll;

import org.example.greenybackend.common.util.ImageStorageService;
import org.example.greenybackend.common.util.ImageStorageService.StoredImage;
import org.example.greenybackend.common.util.ImageDataUris;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.EntityNotFoundException;
import org.example.greenybackend.domain.entity.PlantCareArticles;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.plant.AdminArticleService;
import org.example.greenybackend.modules.plant.PlantCareArticleRepository;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleRequest;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminArticleServiceImpl implements AdminArticleService {

    private final PlantCareArticleRepository articleRepository;
    private final ImageStorageService imageStorageService;

    public AdminArticleServiceImpl(
            PlantCareArticleRepository articleRepository,
            ImageStorageService imageStorageService
    ) {
        this.articleRepository = articleRepository;
        this.imageStorageService = imageStorageService;
    }

    @Override
    public List<PlantCareArticleResponse> getAllArticles() {
        return getAllArticles(null, null, null, null);
    }

    @Override
    public List<PlantCareArticleResponse> getAllArticles(String title, String slug, String status, LocalDate created) {
        return articleRepository.findAll().stream()
                .sorted(Comparator.comparing(PlantCareArticles::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(article -> contains(article.getTitle(), title))
                .filter(article -> contains(article.getSlug(), slug))
                .filter(article -> matchesStatus(status))
                .filter(article -> dateEquals(article.getCreatedAt(), created))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PlantCareArticleResponse getArticle(String articleId) {
        return toResponse(findArticle(articleId));
    }

    @Transactional
    @Override
    public PlantCareArticleResponse createArticle(PlantCareArticleRequest request, UserEntity author) {
        return createArticle(request, author, null);
    }

    @Transactional
    @Override
    public PlantCareArticleResponse createArticle(
            PlantCareArticleRequest request,
            UserEntity author,
            MultipartFile thumbnailFile
    ) {
        validateRequest(request, null);

        LocalDateTime now = LocalDateTime.now();
        PlantCareArticles article = new PlantCareArticles();
        article.setPlantCareArticlesId(UUID.randomUUID().toString());
        article.setUserEntity(author);
        applyRequest(article, request);
        applyThumbnail(article, imageStorageService.read(thumbnailFile));
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        return toResponse(articleRepository.save(article));
    }

    @Transactional
    @Override
    public PlantCareArticleResponse updateArticle(String articleId, PlantCareArticleRequest request) {
        return updateArticle(articleId, request, null);
    }

    @Transactional
    @Override
    public PlantCareArticleResponse updateArticle(String articleId, PlantCareArticleRequest request, MultipartFile thumbnailFile) {
        validateRequest(request, articleId);

        PlantCareArticles article = findArticle(articleId);
        applyRequest(article, request);
        applyThumbnail(article, imageStorageService.read(thumbnailFile));
        article.setUpdatedAt(LocalDateTime.now());
        return toResponse(article);
    }

    @Transactional
    @Override
    public void deleteArticle(String articleId) {
        articleRepository.delete(findArticle(articleId));
    }

    private PlantCareArticles findArticle(String articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bai viet"));
    }

    private boolean matchesStatus(String status) {
        return isBlankOrAll(status) || "stored".equalsIgnoreCase(status.trim());
    }

    private void applyRequest(PlantCareArticles article, PlantCareArticleRequest request) {
        article.setTitle(trimToNull(request.title()));
        article.setSlug(normalizeSlug(request.slug(), request.title()));
        article.setExcerpt(trimToNull(request.excerpt()));
        article.setContent(trimToNull(request.content()));
    }

    private void applyThumbnail(PlantCareArticles article, StoredImage image) {
        if (image == null) {
            return;
        }
        article.setThumbnailData(image.data());
        article.setThumbnailContentType(image.contentType());
        article.setThumbnailFileName(image.fileName());
        article.setThumbnailSize(image.size());
    }

    private PlantCareArticleResponse toResponse(PlantCareArticles article) {
        String authorId = article.getUserId();
        String authorName = null;
        String authorEmail = null;
        try {
            UserEntity author = article.getUserEntity();
            if (author != null) {
                authorId = author.getUserId();
                authorName = author.getDisplayName();
                authorEmail = author.getEmail();
            }
        } catch (EntityNotFoundException exception) {
            authorName = null;
            authorEmail = null;
        }

        return new PlantCareArticleResponse(
                article.getPlantCareArticlesId(),
                article.getTitle(),
                article.getSlug(),
                article.getExcerpt(),
                article.getContent(),
                ImageDataUris.articleThumbnail(article),
                authorId,
                authorName,
                authorEmail,
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private void validateRequest(PlantCareArticleRequest request, String currentArticleId) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Tieu de bai viet khong duoc de trong");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Noi dung bai viet khong duoc de trong");
        }
        String slug = normalizeSlug(request.slug(), request.title());
        if (slug == null) {
            throw new IllegalArgumentException("Slug bai viet khong hop le");
        }
        articleRepository.findBySlug(slug)
                .filter(existing -> !existing.getPlantCareArticlesId().equals(currentArticleId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Slug bai viet da ton tai");
                });
    }

    private String normalizeSlug(String slug, String fallbackTitle) {
        String source = slug == null || slug.isBlank() ? fallbackTitle : slug;
        if (source == null || source.isBlank()) {
            return null;
        }
        String withoutAccent = Normalizer.normalize(source.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalized = withoutAccent
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
