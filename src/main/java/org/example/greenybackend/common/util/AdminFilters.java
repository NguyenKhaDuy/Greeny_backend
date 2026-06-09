package org.example.greenybackend.common.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

//hàm lọc dùng chung
public final class AdminFilters {

    private AdminFilters() {
    }

    public static boolean isBlankOrAll(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim());
    }

    public static boolean contains(String value, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return normalize(value).contains(normalize(query));
    }

    public static boolean equalsText(String value, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return normalize(value).equals(normalize(expected));
    }

    public static boolean dateEquals(LocalDateTime value, LocalDate expectedDate) {
        return expectedDate == null || (value != null && value.toLocalDate().equals(expectedDate));
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccent = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent.toLowerCase(Locale.ROOT);
    }
}
