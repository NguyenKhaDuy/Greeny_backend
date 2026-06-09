package org.example.greenybackend.modules.admin;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.modules.admin.dto.ProductStatisticResponse;
import org.example.greenybackend.modules.admin.dto.RevenueDashboardResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/products")
    public List<ProductStatisticResponse> productStatistics() {
        return analyticsService.getProductStatistics();
    }

    @GetMapping("/revenue")
    public RevenueDashboardResponse revenueDashboard(
            @RequestParam(required = false, defaultValue = "14") Integer days,
            @RequestParam(required = false, defaultValue = "14_days") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return analyticsService.getRevenueDashboard(days, range, start, end);
    }
}
