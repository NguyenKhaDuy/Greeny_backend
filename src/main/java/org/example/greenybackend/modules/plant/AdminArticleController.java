package org.example.greenybackend.modules.plant;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleRequest;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final AdminArticleService articleService;

    public AdminArticleController(AdminArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<PlantCareArticleResponse> getAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate created
    ) {
        return articleService.getAllArticles(title, slug, status, created);
    }

    @GetMapping("/{articleId}")
    public PlantCareArticleResponse getById(@PathVariable String articleId) {
        return articleService.getArticle(articleId);
    }

    @PostMapping
    public ResponseEntity<PlantCareArticleResponse> create(
            @AuthenticationPrincipal(expression = "user") UserEntity author,
            @RequestBody PlantCareArticleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.createArticle(request, author));
    }

    @PutMapping("/{articleId}")
    public PlantCareArticleResponse update(
            @PathVariable String articleId,
            @RequestBody PlantCareArticleRequest request
    ) {
        return articleService.updateArticle(articleId, request);
    }

    @DeleteMapping("/{articleId}")
    public MessageResponse delete(@PathVariable String articleId) {
        articleService.deleteArticle(articleId);
        return new MessageResponse("Da xoa bai viet.");
    }
}
