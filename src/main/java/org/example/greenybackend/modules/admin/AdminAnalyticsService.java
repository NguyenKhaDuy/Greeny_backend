package org.example.greenybackend.modules.admin;

import java.util.List;
import java.time.LocalDate;
import org.example.greenybackend.modules.admin.dto.ProductStatisticResponse;
import org.example.greenybackend.modules.admin.dto.RevenueDashboardResponse;

public interface AdminAnalyticsService {

    List<ProductStatisticResponse> getProductStatistics();

    RevenueDashboardResponse getRevenueDashboard();

    RevenueDashboardResponse getRevenueDashboard(Integer dashboardDays);

    RevenueDashboardResponse getRevenueDashboard(Integer dashboardDays, String revenueRange, LocalDate revenueStart, LocalDate revenueEnd);

}
