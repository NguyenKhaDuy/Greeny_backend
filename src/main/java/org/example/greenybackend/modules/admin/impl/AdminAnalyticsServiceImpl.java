package org.example.greenybackend.modules.admin.impl;

import org.example.greenybackend.modules.admin.AdminAnalyticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.example.greenybackend.domain.entity.OrderItems;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.Payments;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductReviews;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.admin.dto.DashboardTrendResponse;
import org.example.greenybackend.modules.admin.dto.ProductStatisticResponse;
import org.example.greenybackend.modules.admin.dto.RevenueBreakdownResponse;
import org.example.greenybackend.modules.admin.dto.RevenueDailyResponse;
import org.example.greenybackend.modules.admin.dto.RevenueDashboardResponse;
import org.example.greenybackend.modules.admin.dto.RevenueProductContributionResponse;
import org.example.greenybackend.modules.admin.dto.RevenueRecentOrderResponse;
import org.example.greenybackend.modules.admin.dto.RevenueSlowProductResponse;
import org.example.greenybackend.modules.order.AdminOrderService;
import org.example.greenybackend.modules.order.dto.AdminOrderResponse;
import org.example.greenybackend.modules.order.OrdersRepository;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.example.greenybackend.modules.review.ProductReviewsRepository;
import org.example.greenybackend.modules.user.UserRepository;
import org.example.greenybackend.modules.variant.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final int PAYMENT_STATUS_PAID = 1;
    private static final int ORDER_STATUS_CANCELLED = 5;
    private static final int DEFAULT_DASHBOARD_DAYS = 14;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdersRepository ordersRepository;
    private final PlantRepository plantRepository;
    private final ProductReviewsRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final AdminOrderService orderService;

    public AdminAnalyticsServiceImpl(
            OrdersRepository ordersRepository,
            PlantRepository plantRepository,
            ProductReviewsRepository reviewRepository,
            UserRepository userRepository,
            ProductVariantRepository variantRepository,
            AdminOrderService orderService
    ) {
        this.ordersRepository = ordersRepository;
        this.plantRepository = plantRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductStatisticResponse> getProductStatistics() {
        List<ProductStatisticResponse> plantStatistics = getPlantProductStatistics();
        List<ProductStatisticResponse> categoryStatistics = groupedStatistics(plantStatistics, "CATEGORY");
        List<ProductStatisticResponse> plantTypeStatistics = groupedStatistics(plantStatistics, "PLANT_TYPE");

        return java.util.stream.Stream.of(categoryStatistics, plantTypeStatistics, plantStatistics)
                .flatMap(List::stream)
                .toList();
    }

    private List<ProductStatisticResponse> getPlantProductStatistics() {
        List<Orders> paidOrders = paidOrders();
        Map<String, ProductAccumulator> accumulators = new HashMap<>();
        Map<String, List<ProductReviews>> reviewsByPlant = reviewRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(review -> review.getPlant() == null ? "" : review.getPlant().getPlantId()));

        plantRepository.findAll().forEach(plant -> {
            ProductAccumulator accumulator = accumulators.computeIfAbsent(plant.getPlantId(), key -> new ProductAccumulator(plant));
            accumulator.variantCount = plant.getProductVariants() == null ? 0 : plant.getProductVariants().size();
            accumulator.stockQuantity = plant.getProductVariants() == null ? 0 : plant.getProductVariants().stream()
                    .map(ProductVariant::getQuantity)
                    .filter(quantity -> quantity != null)
                    .reduce(0, Integer::sum);
        });

        for (Orders order : paidOrders) {
            for (OrderItems item : order.getOrderItemsList()) {
                ProductVariant variant = item.getProductVariant();
                Plant plant = variant == null ? null : variant.getPlant();
                if (plant == null) {
                    continue;
                }
                ProductAccumulator accumulator = accumulators.computeIfAbsent(plant.getPlantId(), key -> new ProductAccumulator(plant));
                accumulator.soldQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
                accumulator.revenue = accumulator.revenue.add(money(item.getTotalPrice()));
            }
        }

        reviewsByPlant.forEach((plantId, reviews) -> {
            ProductAccumulator accumulator = accumulators.get(plantId);
            if (accumulator != null) {
                accumulator.reviewCount = reviews.size();
                accumulator.averageRating = averageRating(reviews);
            }
        });

        return accumulators.values().stream()
                .map(ProductAccumulator::toResponse)
                .sorted(Comparator.comparing(ProductStatisticResponse::revenue, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProductStatisticResponse::soldQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<ProductStatisticResponse> groupedStatistics(List<ProductStatisticResponse> plantStatistics, String level) {
        Map<String, ProductGroupAccumulator> groups = new HashMap<>();
        plantStatistics.forEach(product -> {
            String key = "CATEGORY".equals(level)
                    ? valueOrDefault(product.categoryTitle(), "Chưa phân loại")
                    : valueOrDefault(product.plantTypeLabel(), "Chưa gán kiểu");
            ProductGroupAccumulator group = groups.computeIfAbsent(key, ignored -> new ProductGroupAccumulator(level, key));
            group.add(product);
        });

        return groups.values().stream()
                .map(ProductGroupAccumulator::toResponse)
                .sorted(Comparator.comparing(ProductStatisticResponse::revenue, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProductStatisticResponse::soldQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public RevenueDashboardResponse getRevenueDashboard() {
        return getRevenueDashboard(DEFAULT_DASHBOARD_DAYS);
    }

    @Transactional(readOnly = true)
    @Override
    public RevenueDashboardResponse getRevenueDashboard(Integer dashboardDays) {
        return getRevenueDashboard(dashboardDays, null, null, null);
    }

    @Transactional(readOnly = true)
    @Override
    public RevenueDashboardResponse getRevenueDashboard(Integer dashboardDays, String revenueRange, LocalDate revenueStart, LocalDate revenueEnd) {
        int days = normalizeDashboardDays(dashboardDays);
        List<Orders> orders = ordersRepository.findAll();
        List<UserEntity> users = userRepository.findAll();
        List<ProductVariant> variants = variantRepository.findAll();
        List<Orders> paidOrders = orders.stream()
                .filter(this::isPaid)
                .toList();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yesterday = today.minusDays(1);
        LocalDate previousMonthStart = monthStart.minusMonths(1);
        LocalDate previousMonthEnd = previousMonthStart
                .plusDays(Math.min(today.getDayOfMonth(), previousMonthStart.lengthOfMonth()) - 1L);
        LocalDate currentStart = today.minusDays(days - 1L);
        LocalDate previousEnd = currentStart.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1L);
        RevenueWindow revenueWindow = resolveRevenueWindow(revenueRange, revenueStart, revenueEnd, today);
        RevenueWindow previousRevenueWindow = previousWindow(revenueWindow);

        BigDecimal totalRevenue = paidOrders.stream().map(order -> money(order.getTotalPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayRevenue = paidOrders.stream()
                .filter(order -> sameDate(order, today))
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayRevenue = paidOrders.stream()
                .filter(order -> sameDate(order, yesterday))
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthRevenue = paidOrders.stream()
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().toLocalDate().isBefore(monthStart))
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousMonthRevenue = paidOrders.stream()
                .filter(order -> inDateRange(order.getCreatedAt(), previousMonthStart, previousMonthEnd))
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountTotal = paidOrders.stream().map(order -> money(order.getDiscountAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFeeTotal = paidOrders.stream().map(order -> money(order.getShippingFee())).reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer soldQuantity = paidOrders.stream()
                .flatMap(order -> orderItems(order).stream())
                .map(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .reduce(0, Integer::sum);

        List<Orders> currentPeriodOrders = orders.stream()
                .filter(order -> inDateRange(order.getCreatedAt(), currentStart, today))
                .toList();
        List<Orders> previousPeriodOrders = orders.stream()
                .filter(order -> inDateRange(order.getCreatedAt(), previousStart, previousEnd))
                .toList();
        List<Orders> currentPeriodPaidOrders = currentPeriodOrders.stream()
                .filter(this::isPaid)
                .toList();
        List<Orders> previousPeriodPaidOrders = previousPeriodOrders.stream()
                .filter(this::isPaid)
                .toList();
        BigDecimal periodRevenue = currentPeriodPaidOrders.stream()
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousPeriodRevenue = previousPeriodPaidOrders.stream()
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int periodUsers = (int) users.stream()
                .filter(user -> inDateRange(user.getCreateat(), currentStart, today))
                .count();
        int previousPeriodUsers = (int) users.stream()
                .filter(user -> inDateRange(user.getCreateat(), previousStart, previousEnd))
                .count();
        int lowStockItems = (int) variants.stream()
                .filter(this::isLowStock)
                .count();

        List<Orders> rangeOrders = orders.stream()
                .filter(order -> inDateRange(order.getCreatedAt(), revenueWindow.start(), revenueWindow.end()))
                .toList();
        List<Orders> previousRangeOrders = orders.stream()
                .filter(order -> inDateRange(order.getCreatedAt(), previousRevenueWindow.start(), previousRevenueWindow.end()))
                .toList();
        List<Orders> rangePaidOrders = rangeOrders.stream()
                .filter(this::isPaid)
                .toList();
        List<Orders> previousRangePaidOrders = previousRangeOrders.stream()
                .filter(this::isPaid)
                .toList();
        BigDecimal rangeRevenue = rangePaidOrders.stream()
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousRangeRevenue = previousRangePaidOrders.stream()
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int rangeCancelledOrders = (int) rangeOrders.stream().filter(this::isCancelled).count();
        int previousRangeCancelledOrders = (int) previousRangeOrders.stream().filter(this::isCancelled).count();
        int rangeUnpaidOrders = (int) rangeOrders.stream().filter(this::isAwaitingPayment).count();
        BigDecimal rangeAverageOrderValue = averageOrderValue(rangeRevenue, rangePaidOrders.size());
        BigDecimal previousRangeAverageOrderValue = averageOrderValue(previousRangeRevenue, previousRangePaidOrders.size());
        int rangePaymentRatePercent = percentOf(rangePaidOrders.size(), rangeOrders.size());
        int previousRangePaymentRatePercent = percentOf(previousRangePaidOrders.size(), previousRangeOrders.size());

        List<RevenueDailyResponse> dailyRevenue = dailyRevenue(paidOrders, days);
        List<RevenueDailyResponse> fourteenDayRevenue = dailyRevenue(paidOrders, DEFAULT_DASHBOARD_DAYS);
        List<ProductStatisticResponse> topProducts = getPlantProductStatistics().stream().limit(6).toList();
        List<AdminOrderResponse> recentPaidOrders = paidOrders.stream()
                .sorted(Comparator.comparing(Orders::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(order -> orderService.getOrder(order.getOrderId()))
                .toList();

        int paidCount = paidOrders.size();
        int totalOrders = orders.size();
        BigDecimal averageOrderValue = averageOrderValue(totalRevenue, paidCount);
        return new RevenueDashboardResponse(
                totalRevenue,
                todayRevenue,
                monthRevenue,
                averageOrderValue,
                discountTotal,
                shippingFeeTotal,
                totalOrders,
                paidCount,
                (int) orders.stream().filter(order -> order.getPaymentStatus() == null || order.getPaymentStatus() != PAYMENT_STATUS_PAID).count(),
                (int) orders.stream().filter(order -> order.getStatus() != null && order.getStatus() == ORDER_STATUS_CANCELLED).count(),
                soldQuantity,
                totalOrders == 0 ? 0 : BigDecimal.valueOf(paidCount * 100L).divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP).intValue(),
                dailyRevenue,
                topProducts,
                recentPaidOrders,
                days,
                currentPeriodOrders.size(),
                currentPeriodPaidOrders.size(),
                periodRevenue,
                periodUsers,
                numericTrend(currentPeriodOrders.size(), previousPeriodOrders.size(), days, true),
                moneyTrend(periodRevenue, previousPeriodRevenue, days),
                numericTrend(periodUsers, previousPeriodUsers, days, true),
                inventoryTrend(lowStockItems, variants.size()),
                revenueWindow.key(),
                revenueWindow.label(),
                revenueWindow.start(),
                revenueWindow.end(),
                formatWindow(previousRevenueWindow),
                rangeRevenue,
                rangeOrders.size(),
                rangePaidOrders.size(),
                rangeUnpaidOrders,
                rangeCancelledOrders,
                rangeAverageOrderValue,
                rangePaymentRatePercent,
                fourteenDayRevenue,
                statusBreakdown(rangeOrders),
                paymentMethodBreakdown(rangePaidOrders, rangeRevenue),
                topRevenueProducts(rangePaidOrders, rangeRevenue),
                slowProducts(variants, rangePaidOrders),
                topValueOrderSummaries(rangePaidOrders),
                trendWithLabel(rangeRevenue, previousRangeRevenue, "kỳ trước", true),
                trendWithLabel(todayRevenue, yesterdayRevenue, "hôm qua", true),
                trendWithLabel(monthRevenue, previousMonthRevenue, "cùng kỳ tháng trước", true),
                trendWithLabel(rangeAverageOrderValue, previousRangeAverageOrderValue, "kỳ trước", true),
                trendWithLabel(BigDecimal.valueOf(rangePaymentRatePercent), BigDecimal.valueOf(previousRangePaymentRatePercent), "kỳ trước", true),
                trendWithLabel(BigDecimal.valueOf(rangeCancelledOrders), BigDecimal.valueOf(previousRangeCancelledOrders), "kỳ trước", false)
        );
    }

    private List<RevenueDailyResponse> dailyRevenue(List<Orders> paidOrders, int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        Map<LocalDate, BigDecimal> revenueByDate = new HashMap<>();
        Map<LocalDate, Integer> ordersByDate = new HashMap<>();
        Map<LocalDate, Integer> soldByDate = new HashMap<>();

        paidOrders.forEach(order -> {
            if (order.getCreatedAt() == null) {
                return;
            }
            LocalDate date = order.getCreatedAt().toLocalDate();
            if (date.isBefore(start)) {
                return;
            }
            revenueByDate.merge(date, money(order.getTotalPrice()), BigDecimal::add);
            ordersByDate.merge(date, 1, Integer::sum);
            int sold = orderItems(order).stream()
                    .map(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                    .reduce(0, Integer::sum);
            soldByDate.merge(date, sold, Integer::sum);
        });

        BigDecimal peak = revenueByDate.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(start::plusDays)
                .map(date -> {
                    BigDecimal revenue = revenueByDate.getOrDefault(date, BigDecimal.ZERO);
                    int percent = peak.compareTo(BigDecimal.ZERO) == 0
                            ? 0
                            : revenue.multiply(BigDecimal.valueOf(100)).divide(peak, 0, RoundingMode.HALF_UP).intValue();
                    return new RevenueDailyResponse(
                            date,
                            revenue,
                            ordersByDate.getOrDefault(date, 0),
                            soldByDate.getOrDefault(date, 0),
                            percent
                    );
                })
                .toList();
    }

    private List<RevenueBreakdownResponse> statusBreakdown(List<Orders> rangeOrders) {
        int total = rangeOrders.size();
        List<Orders> paid = rangeOrders.stream().filter(this::isPaid).toList();
        List<Orders> cancelled = rangeOrders.stream().filter(this::isCancelled).toList();
        List<Orders> awaiting = rangeOrders.stream().filter(this::isAwaitingPayment).toList();

        return List.of(
                breakdown("paid", "Đã thanh toán", paid, total, "positive"),
                breakdown("pending", "Chờ thanh toán", awaiting, total, "warning"),
                breakdown("cancelled", "Đã hủy", cancelled, total, "negative")
        );
    }

    private RevenueBreakdownResponse breakdown(String key, String label, List<Orders> orders, int totalOrders, String tone) {
        BigDecimal revenue = orders.stream()
                .map(order -> money(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RevenueBreakdownResponse(key, label, orders.size(), revenue, percentOf(orders.size(), totalOrders), tone);
    }

    private List<RevenueBreakdownResponse> paymentMethodBreakdown(List<Orders> rangePaidOrders, BigDecimal rangeRevenue) {
        Map<String, PaymentMethodAccumulator> methods = new LinkedHashMap<>();
        for (Orders order : rangePaidOrders) {
            String method = order.getPayments() == null ? null : order.getPayments().getMethod();
            String key = normalizedPaymentMethodKey(method);
            if (key == null) {
                continue;
            }
            PaymentMethodAccumulator accumulator = methods.computeIfAbsent(key, ignored -> new PaymentMethodAccumulator(paymentMethodLabel(method)));
            accumulator.count += 1;
            accumulator.revenue = accumulator.revenue.add(money(order.getTotalPrice()));
        }
        BigDecimal allowedRevenue = methods.values().stream()
                .map(accumulator -> accumulator.revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return methods.entrySet().stream()
                .map(entry -> new RevenueBreakdownResponse(
                        entry.getKey(),
                        entry.getValue().label,
                        entry.getValue().count,
                        entry.getValue().revenue,
                        percentOf(entry.getValue().revenue, allowedRevenue),
                        "positive"
                ))
                .sorted(Comparator.comparing(RevenueBreakdownResponse::revenue, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<RevenueProductContributionResponse> topRevenueProducts(List<Orders> rangePaidOrders, BigDecimal rangeRevenue) {
        Map<String, ProductRevenueAccumulator> products = new HashMap<>();
        for (Orders order : rangePaidOrders) {
            for (OrderItems item : orderItems(order)) {
                ProductVariant variant = item.getProductVariant();
                Plant plant = variant == null ? null : variant.getPlant();
                if (plant == null) {
                    continue;
                }
                ProductRevenueAccumulator accumulator = products.computeIfAbsent(plant.getPlantId(), ignored -> new ProductRevenueAccumulator(plant));
                accumulator.soldQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
                accumulator.revenue = accumulator.revenue.add(money(item.getTotalPrice()));
            }
        }

        return products.values().stream()
                .map(accumulator -> new RevenueProductContributionResponse(
                        accumulator.plantId,
                        accumulator.plantTitle,
                        accumulator.categoryTitle,
                        accumulator.soldQuantity,
                        accumulator.stockQuantity,
                        accumulator.revenue,
                        percentOf(accumulator.revenue, rangeRevenue)
                ))
                .sorted(Comparator.comparing(RevenueProductContributionResponse::revenue, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .toList();
    }

    private List<RevenueSlowProductResponse> slowProducts(List<ProductVariant> variants, List<Orders> rangePaidOrders) {
        Map<String, VariantSalesAccumulator> salesByVariant = new HashMap<>();
        for (Orders order : rangePaidOrders) {
            for (OrderItems item : orderItems(order)) {
                ProductVariant variant = item.getProductVariant();
                if (variant == null || variant.getVariantId() == null) {
                    continue;
                }
                VariantSalesAccumulator accumulator = salesByVariant.computeIfAbsent(variant.getVariantId(), ignored -> new VariantSalesAccumulator());
                accumulator.soldQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
                accumulator.revenue = accumulator.revenue.add(money(item.getTotalPrice()));
            }
        }

        return variants.stream()
                .map(variant -> slowProduct(variant, salesByVariant.get(variant.getVariantId())))
                .filter(product -> (product.stockQuantity() >= 5 && product.soldQuantity() <= 1)
                        || (product.stockQuantity() >= 12 && product.soldQuantity() <= 3))
                .sorted(Comparator.comparing(RevenueSlowProductResponse::soldQuantity, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RevenueSlowProductResponse::stockQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .toList();
    }

    private RevenueSlowProductResponse slowProduct(ProductVariant variant, VariantSalesAccumulator sales) {
        int stock = variant.getQuantity() == null ? 0 : variant.getQuantity();
        int sold = sales == null ? 0 : sales.soldQuantity;
        BigDecimal revenue = sales == null ? BigDecimal.ZERO : sales.revenue;
        String recommendation = slowProductRecommendation(stock, sold);
        return new RevenueSlowProductResponse(
                variant.getVariantId(),
                variant.getPlant() == null ? "Chưa có tên sản phẩm" : valueOrDefault(variant.getPlant().getTitle(), "Chưa có tên sản phẩm"),
                valueOrDefault(variant.getName(), "Biến thể chưa đặt tên"),
                valueOrDefault(variant.getSku(), "Chưa có SKU"),
                stock,
                sold,
                revenue,
                recommendation,
                sold == 0 && stock >= 12 ? "negative" : "warning"
        );
    }

    private String slowProductRecommendation(int stock, int sold) {
        if (sold == 0 && stock >= 20) {
            return "Ưu tiên khuyến mãi mạnh";
        }
        if (sold == 0) {
            return "Đề xuất combo hoặc giảm giá";
        }
        if (stock >= 12) {
            return "Cần đẩy hiển thị";
        }
        return "Theo dõi thêm";
    }

    private List<RevenueRecentOrderResponse> topValueOrderSummaries(List<Orders> rangePaidOrders) {
        return rangePaidOrders.stream()
                .sorted(Comparator.comparing((Orders order) -> money(order.getTotalPrice()), Comparator.reverseOrder())
                        .thenComparing(this::paidAtOrCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(order -> new RevenueRecentOrderResponse(
                        order.getOrderId(),
                        customerName(order),
                        paidAtOrCreatedAt(order),
                        paymentMethodLabel(order.getPayments() == null ? null : order.getPayments().getMethod()),
                        money(order.getTotalPrice())
                ))
                .toList();
    }

    private LocalDateTime paidAtOrCreatedAt(Orders order) {
        Payments payment = order.getPayments();
        if (payment != null && payment.getPaidAt() != null) {
            return payment.getPaidAt();
        }
        return order.getCreatedAt();
    }

    private String customerName(Orders order) {
        if (order.getUserEntity() == null) {
            return "Khách hàng";
        }
        return valueOrDefault(order.getUserEntity().getDisplayName(), "Khách hàng");
    }

    private DashboardTrendResponse numericTrend(int current, int previous, int days, boolean higherIsBetter) {
        return trend(BigDecimal.valueOf(current), BigDecimal.valueOf(previous), days, higherIsBetter);
    }

    private DashboardTrendResponse moneyTrend(BigDecimal current, BigDecimal previous, int days) {
        return trend(money(current), money(previous), days, true);
    }

    private DashboardTrendResponse trend(BigDecimal current, BigDecimal previous, int days, boolean higherIsBetter) {
        return trendWithLabel(current, previous, days + " ngày trước", higherIsBetter);
    }

    private DashboardTrendResponse trendWithLabel(BigDecimal current, BigDecimal previous, String comparisonLabel, boolean higherIsBetter) {
        int percent = percentChange(current, previous);
        String prefix = percent > 0 ? "+" : "";
        String label = prefix + percent + "% so với " + comparisonLabel;
        String tone = trendTone(percent, higherIsBetter);
        return new DashboardTrendResponse(percent, label, tone);
    }

    private DashboardTrendResponse inventoryTrend(int lowStockItems, int totalItems) {
        int percent = totalItems == 0
                ? 0
                : BigDecimal.valueOf(lowStockItems * 100L)
                .divide(BigDecimal.valueOf(totalItems), 0, RoundingMode.HALF_UP)
                .intValue();
        String label = percent + "% mặt hàng cần chú ý";
        String tone = lowStockItems > 0 ? "warning" : "positive";
        return new DashboardTrendResponse(percent, label, tone);
    }

    private int percentChange(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = money(current);
        BigDecimal safePrevious = money(previous);
        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100;
        }
        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String trendTone(int percent, boolean higherIsBetter) {
        if (percent == 0) {
            return "neutral";
        }
        boolean positive = percent > 0;
        if (!higherIsBetter) {
            positive = !positive;
        }
        return positive ? "positive" : "negative";
    }

    private RevenueWindow resolveRevenueWindow(String revenueRange, LocalDate revenueStart, LocalDate revenueEnd, LocalDate today) {
        String range = normalizeRevenueRange(revenueRange);
        return switch (range) {
            case "today" -> new RevenueWindow("today", "Hôm nay", today, today);
            case "7_days" -> new RevenueWindow("7_days", "7 ngày", today.minusDays(6), today);
            case "month" -> new RevenueWindow("month", "Tháng này", today.withDayOfMonth(1), today);
            case "year" -> new RevenueWindow("year", "Năm nay", LocalDate.of(today.getYear(), 1, 1), today);
            case "custom" -> customRevenueWindow(revenueStart, revenueEnd, today);
            default -> new RevenueWindow("14_days", "14 ngày", today.minusDays(13), today);
        };
    }

    private RevenueWindow customRevenueWindow(LocalDate revenueStart, LocalDate revenueEnd, LocalDate today) {
        if (revenueStart == null || revenueEnd == null) {
            return new RevenueWindow("14_days", "14 ngày", today.minusDays(13), today);
        }
        LocalDate start = revenueStart.isAfter(revenueEnd) ? revenueEnd : revenueStart;
        LocalDate end = revenueStart.isAfter(revenueEnd) ? revenueStart : revenueEnd;
        return new RevenueWindow("custom", "Tùy chỉnh ngày", start, end);
    }

    private String normalizeRevenueRange(String revenueRange) {
        if (revenueRange == null || revenueRange.isBlank()) {
            return "14_days";
        }
        String normalized = revenueRange.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "today", "7_days", "7", "14_days", "14", "month", "year", "custom" ->
                    normalized.equals("7") ? "7_days" : normalized.equals("14") ? "14_days" : normalized;
            default -> "14_days";
        };
    }

    private RevenueWindow previousWindow(RevenueWindow currentWindow) {
        long days = ChronoUnit.DAYS.between(currentWindow.start(), currentWindow.end()) + 1;
        LocalDate end = currentWindow.start().minusDays(1);
        LocalDate start = end.minusDays(days - 1);
        return new RevenueWindow("previous", "Kỳ trước", start, end);
    }

    private String formatWindow(RevenueWindow window) {
        return DISPLAY_DATE_FORMATTER.format(window.start()) + " - " + DISPLAY_DATE_FORMATTER.format(window.end());
    }

    private BigDecimal averageOrderValue(BigDecimal revenue, int paidOrders) {
        return paidOrders == 0 ? BigDecimal.ZERO : money(revenue).divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);
    }

    private int percentOf(int part, int total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf(part * 100L)
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int percentOf(BigDecimal part, BigDecimal total) {
        BigDecimal safeTotal = money(total);
        if (safeTotal.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return money(part).multiply(BigDecimal.valueOf(100))
                .divide(safeTotal, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private List<OrderItems> orderItems(Orders order) {
        return order.getOrderItemsList() == null ? List.of() : order.getOrderItemsList();
    }

    private boolean isCancelled(Orders order) {
        return order.getStatus() != null && order.getStatus() == ORDER_STATUS_CANCELLED;
    }

    private boolean isAwaitingPayment(Orders order) {
        return !isPaid(order) && !isCancelled(order);
    }

    private String paymentMethodLabel(String method) {
        String key = normalizedPaymentMethodKey(method);
        if (key == null) {
            return "Chưa có dữ liệu";
        }
        return switch (key) {
            case "COD" -> "COD";
            case "BANK_TRANSFER" -> "Chuyển khoản";
            default -> "Chưa có dữ liệu";
        };
    }

    private String normalizedPaymentMethodKey(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
        if (normalized.equals("COD") || normalized.equals("CODE") || normalized.contains("CASH_ON_DELIVERY")) {
            return "COD";
        }
        if (normalized.equals("BANK")
                || normalized.equals("BANK_TRANSFER")
                || normalized.equals("TRANSFER")
                || normalized.contains("CHUYEN_KHOAN")
                || normalized.contains("CHUYỂN_KHOẢN")) {
            return "BANK_TRANSFER";
        }
        return null;
    }

    private boolean inDateRange(LocalDateTime value, LocalDate start, LocalDate end) {
        if (value == null) {
            return false;
        }
        LocalDate date = value.toLocalDate();
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean isPaid(Orders order) {
        return order.getPaymentStatus() != null && order.getPaymentStatus() == PAYMENT_STATUS_PAID;
    }

    private boolean isLowStock(ProductVariant variant) {
        return variant.getQuantity() == null || variant.getQuantity() <= 5;
    }

    private int normalizeDashboardDays(Integer dashboardDays) {
        if (dashboardDays == null) {
            return DEFAULT_DASHBOARD_DAYS;
        }
        return switch (dashboardDays) {
            case 7, 14, 30 -> dashboardDays;
            default -> DEFAULT_DASHBOARD_DAYS;
        };
    }

    private List<Orders> paidOrders() {
        return ordersRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();
    }

    private boolean sameDate(Orders order, LocalDate date) {
        return order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().equals(date);
    }

    private BigDecimal averageRating(List<ProductReviews> reviews) {
        List<Integer> ratings = reviews.stream()
                .map(ProductReviews::getRating)
                .filter(rating -> rating != null)
                .toList();
        if (ratings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = ratings.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String plantTypeLabel(Integer plantType) {
        if (plantType == null) {
            return "Chưa gán kiểu";
        }
        return "Kiểu cây " + plantType;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record RevenueWindow(String key, String label, LocalDate start, LocalDate end) {
    }

    private static class PaymentMethodAccumulator {
        private final String label;
        private int count;
        private BigDecimal revenue = BigDecimal.ZERO;

        PaymentMethodAccumulator(String label) {
            this.label = label;
        }
    }

    private static class VariantSalesAccumulator {
        private int soldQuantity;
        private BigDecimal revenue = BigDecimal.ZERO;
    }

    private static class ProductRevenueAccumulator {
        private final String plantId;
        private final String plantTitle;
        private final String categoryTitle;
        private final int stockQuantity;
        private int soldQuantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        ProductRevenueAccumulator(Plant plant) {
            this.plantId = plant.getPlantId();
            this.plantTitle = plant.getTitle();
            this.categoryTitle = plant.getCategory() == null ? null : plant.getCategory().getTitle();
            this.stockQuantity = plant.getProductVariants() == null ? 0 : plant.getProductVariants().stream()
                    .map(ProductVariant::getQuantity)
                    .filter(quantity -> quantity != null)
                    .reduce(0, Integer::sum);
        }
    }

    private class ProductAccumulator {
        private final Plant plant;
        private int variantCount;
        private int stockQuantity;
        private int soldQuantity;
        private int reviewCount;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal averageRating = BigDecimal.ZERO;

        ProductAccumulator(Plant plant) {
            this.plant = plant;
        }

        ProductStatisticResponse toResponse() {
            return new ProductStatisticResponse(
                    "PLANT",
                    plant.getPlantId(),
                    plant.getTitle(),
                    plant.getPlantId(),
                    plant.getTitle(),
                    plant.getCategory() == null ? null : plant.getCategory().getTitle(),
                    plant.getPlantType(),
                    plantTypeLabel(plant.getPlantType()),
                    1,
                    variantCount,
                    stockQuantity,
                    soldQuantity,
                    revenue,
                    averageRating,
                    reviewCount
            );
        }
    }

    private static class ProductGroupAccumulator {
        private final String level;
        private final String title;
        private int plantCount;
        private int variantCount;
        private int stockQuantity;
        private int soldQuantity;
        private int reviewCount;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal ratingTotal = BigDecimal.ZERO;
        private int ratingPlants;

        ProductGroupAccumulator(String level, String title) {
            this.level = level;
            this.title = title;
        }

        void add(ProductStatisticResponse product) {
            plantCount += product.plantCount() == null ? 0 : product.plantCount();
            variantCount += product.variantCount() == null ? 0 : product.variantCount();
            stockQuantity += product.stockQuantity() == null ? 0 : product.stockQuantity();
            soldQuantity += product.soldQuantity() == null ? 0 : product.soldQuantity();
            reviewCount += product.reviewCount() == null ? 0 : product.reviewCount();
            revenue = revenue.add(product.revenue() == null ? BigDecimal.ZERO : product.revenue());
            if (product.averageRating() != null && product.averageRating().compareTo(BigDecimal.ZERO) > 0) {
                ratingTotal = ratingTotal.add(product.averageRating());
                ratingPlants += 1;
            }
        }

        ProductStatisticResponse toResponse() {
            BigDecimal averageRating = ratingPlants == 0
                    ? BigDecimal.ZERO
                    : ratingTotal.divide(BigDecimal.valueOf(ratingPlants), 2, RoundingMode.HALF_UP);
            return new ProductStatisticResponse(
                    level,
                    title,
                    title,
                    null,
                    null,
                    "CATEGORY".equals(level) ? title : null,
                    null,
                    "PLANT_TYPE".equals(level) ? title : null,
                    plantCount,
                    variantCount,
                    stockQuantity,
                    soldQuantity,
                    revenue,
                    averageRating,
                    reviewCount
            );
        }
    }
}
