package org.example.greenybackend.modules.ai;

import java.util.List;
import org.example.greenybackend.modules.ai.dto.AiContextDTO;

public record AiContextResult(
        String intent,
        List<String> entities,
        int totalRecords,
        String databaseContext,
        List<String> summaryLines
) {

    public boolean hasDatabaseData() {
        return totalRecords > 0 && databaseContext != null && !databaseContext.isBlank();
    }

    public AiContextDTO toDto() {
        return new AiContextDTO(intent, entities, totalRecords, hasDatabaseData(), summaryLines);
    }
}
