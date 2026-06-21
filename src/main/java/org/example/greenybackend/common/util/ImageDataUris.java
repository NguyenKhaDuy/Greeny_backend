package org.example.greenybackend.common.util;

import java.util.Base64;
import org.example.greenybackend.domain.entity.AiPlantContexts;
import org.example.greenybackend.domain.entity.Category;
import org.example.greenybackend.domain.entity.PlantCareArticles;
import org.example.greenybackend.domain.entity.ProductImage;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.example.greenybackend.domain.entity.UserEntity;

public final class ImageDataUris {

    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/png";

    private ImageDataUris() {
    }

    public static String categoryImage(Category category) {
        if (category == null) {
            return null;
        }
        return dataUri(category.getImageData(), category.getImageContentType());
    }

    public static String articleThumbnail(PlantCareArticles article) {
        if (article == null) {
            return null;
        }
        return dataUri(article.getThumbnailData(), article.getThumbnailContentType());
    }

    public static String productImage(ProductImage image) {
        if (image == null) {
            return null;
        }
        return dataUri(image.getImageData(), image.getImageContentType());
    }

    public static String userAvatar(UserEntity user) {
        if (user == null) {
            return null;
        }
        return dataUri(user.getAvatarData(), user.getAvatarContentType());
    }

    public static String reviewImage(ProductReviews review) {
        if (review == null) {
            return null;
        }
        return dataUri(review.getImagesData(), review.getImagesContentType());
    }

    public static String aiPlantContextImage(AiPlantContexts context) {
        if (context == null) {
            return null;
        }
        return dataUri(context.getImageData(), context.getImageContentType());
    }

    public static String dataUri(byte[] data, String contentType) {
        if (!hasBytes(data)) {
            return null;
        }
        return "data:" + imageContentType(contentType) + ";base64," + Base64.getEncoder().encodeToString(data);
    }

    public static boolean hasBytes(byte[] data) {
        return data != null && data.length > 0;
    }

    private static String imageContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_IMAGE_CONTENT_TYPE;
        }
        String normalized = contentType.trim();
        return normalized.startsWith("image/") ? normalized : DEFAULT_IMAGE_CONTENT_TYPE;
    }
}
