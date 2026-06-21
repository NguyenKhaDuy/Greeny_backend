package org.example.greenybackend.common.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private final long maxFileSizeBytes;

    public ImageStorageService(
            @Value("${greeny.images.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public StoredImage read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Anh khong duoc vuot qua " + maxFileSizeBytes / (1024 * 1024) + "MB");
        }

        String contentType = normalizeContentType(file);
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Chi cho phep upload file anh");
        }

        try {
            return new StoredImage(
                    cleanFileName(file.getOriginalFilename()),
                    contentType,
                    file.getBytes(),
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Khong the doc file anh upload", exception);
        }
    }

    public List<StoredImage> readAll(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return List.of();
        }
        return Arrays.stream(files)
                .map(this::read)
                .filter(image -> image != null && image.data().length > 0)
                .toList();
    }

    private String normalizeContentType(MultipartFile file) {
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            return file.getContentType().trim();
        }
        return MediaTypeFactory.getMediaType(file.getOriginalFilename())
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "image";
        }
        String normalized = fileName.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    public record StoredImage(
            String fileName,
            String contentType,
            byte[] data,
            Long size
    ) {
    }
}
