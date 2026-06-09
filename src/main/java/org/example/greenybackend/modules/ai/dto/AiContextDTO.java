package org.example.greenybackend.modules.ai.dto;

import java.util.List;

public record AiContextDTO(
        String intent,
        List<String> entities,
        int totalRecords,
        boolean hasDatabaseData,
        List<String> summaryLines
) {
}
