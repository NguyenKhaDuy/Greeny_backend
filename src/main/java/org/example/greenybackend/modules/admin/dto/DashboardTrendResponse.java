package org.example.greenybackend.modules.admin.dto;

public record DashboardTrendResponse(
        Integer percent,
        String label,
        String tone
) {
}
