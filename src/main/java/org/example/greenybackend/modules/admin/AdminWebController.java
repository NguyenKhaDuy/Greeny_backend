package org.example.greenybackend.modules.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.category.AdminCatalogService;
import org.example.greenybackend.modules.category.dto.CategoryRequest;
import org.example.greenybackend.modules.category.dto.CategoryResponse;
import org.example.greenybackend.modules.notification.AdminNotificationService;
import org.example.greenybackend.modules.notification.dto.AdminNotificationRequest;
import org.example.greenybackend.modules.notification.dto.AdminNotificationResponse;
import org.example.greenybackend.modules.order.AdminOrderService;
import org.example.greenybackend.modules.order.dto.AdminOrderResponse;
import org.example.greenybackend.modules.order.dto.OrderStatusUpdateRequest;
import org.example.greenybackend.modules.payment.dto.PaymentUpdateRequest;
import org.example.greenybackend.modules.plant.AdminArticleService;
import org.example.greenybackend.modules.plant.AdminCareService;
import org.example.greenybackend.modules.plant.AdminPlantService;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleRequest;
import org.example.greenybackend.modules.plant.dto.PlantCareArticleResponse;
import org.example.greenybackend.modules.plant.dto.PlantCareProfileResponse;
import org.example.greenybackend.modules.plant.dto.PlantRequest;
import org.example.greenybackend.modules.plant.dto.PlantResponse;
import org.example.greenybackend.modules.promotion.AdminCouponService;
import org.example.greenybackend.modules.promotion.dto.CouponRequest;
import org.example.greenybackend.modules.promotion.dto.CouponResponse;
import org.example.greenybackend.modules.review.AdminReviewService;
import org.example.greenybackend.modules.review.dto.AdminReviewResponse;
import org.example.greenybackend.modules.review.dto.ReviewModerationRequest;
import org.example.greenybackend.modules.user.AdminUserService;
import org.example.greenybackend.modules.user.dto.AdminUserResponse;
import org.example.greenybackend.modules.user.dto.AdminUserUpdateRequest;
import org.example.greenybackend.modules.variant.AdminInventoryService;
import org.example.greenybackend.modules.variant.AdminVariantService;
import org.example.greenybackend.modules.variant.dto.ProductVariantRequest;
import org.example.greenybackend.modules.variant.dto.ProductVariantResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminWebController {

    private final AdminCatalogService catalogService;
    private final AdminPlantService plantService;
    private final AdminVariantService variantService;
    private final AdminCareService careService;
    private final AdminInventoryService inventoryService;
    private final AdminCouponService couponService;
    private final AdminArticleService articleService;
    private final AdminOrderService orderService;
    private final AdminNotificationService notificationService;
    private final AdminReviewService reviewService;
    private final AdminAnalyticsService analyticsService;
    private final AdminUserService userService;

    public AdminWebController(
            AdminCatalogService catalogService,
            AdminPlantService plantService,
            AdminVariantService variantService,
            AdminCareService careService,
            AdminInventoryService inventoryService,
            AdminCouponService couponService,
            AdminArticleService articleService,
            AdminOrderService orderService,
            AdminNotificationService notificationService,
            AdminReviewService reviewService,
            AdminAnalyticsService analyticsService,
            AdminUserService userService
    ) {
        this.catalogService = catalogService;
        this.plantService = plantService;
        this.variantService = variantService;
        this.careService = careService;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.articleService = articleService;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.reviewService = reviewService;
        this.analyticsService = analyticsService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/admin";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/admin")
    public String dashboard(
            @RequestParam(required = false, defaultValue = "14") Integer dashboardDays,
            @RequestParam(required = false, defaultValue = "14_days") String revenueRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate revenueStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate revenueEnd,
            Model model
    ) {
        int normalizedDashboardDays = normalizeDashboardDays(dashboardDays);
        List<CouponResponse> coupons = couponService.getAllCoupons();
        List<CouponResponse> systemCoupons = coupons.stream()
                .filter(this::isSystemCoupon)
                .toList();
        List<CouponResponse> regularCoupons = coupons.stream()
                .filter(coupon -> !isSystemCoupon(coupon))
                .toList();
        List<AdminOrderResponse> orders = orderService.getAllOrders(null, null);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusDays(7);
        CouponViewState couponViewState = buildCouponViewState(coupons, now, soon);
        CouponViewState regularCouponViewState = buildCouponViewState(regularCoupons, now, soon);
        List<CategoryResponse> categories = catalogService.getAllCategories();
        List<PlantResponse> plants = plantService.getAllPlants(false);
        List<PlantResponse> catalogPlants = plantService.getAllPlants(true);
        List<ProductVariantResponse> variants = variantService.getAllVariants(null);
        Map<String, Long> categoryPlantCounts = catalogPlants.stream()
                .filter(plant -> plant.categoryId() != null && !plant.categoryId().isBlank())
                .collect(Collectors.groupingBy(PlantResponse::categoryId, Collectors.counting()));
        Map<String, Long> plantVariantCounts = variants.stream()
                .filter(variant -> variant.plantId() != null && !variant.plantId().isBlank())
                .collect(Collectors.groupingBy(ProductVariantResponse::plantId, Collectors.counting()));
        Map<String, String> variantCategoryTitles = catalogPlants.stream()
                .filter(plant -> plant.plantId() != null && !plant.plantId().isBlank())
                .collect(Collectors.toMap(
                        PlantResponse::plantId,
                        plant -> plant.categoryTitle() == null || plant.categoryTitle().isBlank()
                                ? "Chưa chọn danh mục"
                                : plant.categoryTitle(),
                        (left, right) -> left
                ));
        Map<String, String> variantCategoryIds = catalogPlants.stream()
                .filter(plant -> plant.plantId() != null && !plant.plantId().isBlank())
                .collect(Collectors.toMap(
                        PlantResponse::plantId,
                        plant -> plant.categoryId() == null || plant.categoryId().isBlank() ? "none" : plant.categoryId(),
                        (left, right) -> left
                ));
        List<PlantCareProfileResponse> careProfiles = careService.getAllCareProfiles();
        List<ProductVariantResponse> inventoryItems = inventoryService.getInventoryItems();
        List<PlantCareArticleResponse> articles = articleService.getAllArticles();
        List<AdminNotificationResponse> notifications = notificationService.getAllNotifications();
        List<AdminReviewResponse> reviews = reviewService.getAllReviews(null);
        List<AdminUserResponse> users = userService.getAllUsers();
        Set<String> carePlantIds = careProfiles.stream()
                .map(PlantCareProfileResponse::plantId)
                .filter(plantId -> plantId != null && !plantId.isBlank())
                .collect(Collectors.toSet());
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        model.addAttribute("categories", categories);
        model.addAttribute("plants", plants);
        model.addAttribute("catalogPlants", catalogPlants);
        model.addAttribute("variants", variants);
        model.addAttribute("categoryPlantCounts", categoryPlantCounts);
        model.addAttribute("plantVariantCounts", plantVariantCounts);
        model.addAttribute("variantCategoryTitles", variantCategoryTitles);
        model.addAttribute("variantCategoryIds", variantCategoryIds);
        model.addAttribute("categoryTotalCount", categories.size());
        model.addAttribute("categoryActiveCount", categories.stream().filter(category -> Boolean.TRUE.equals(category.isActive())).count());
        model.addAttribute("categoryHiddenCount", categories.stream().filter(category -> !Boolean.TRUE.equals(category.isActive())).count());
        model.addAttribute("categoryWithPlantCount", categoryPlantCounts.values().stream().filter(count -> count > 0).count());
        model.addAttribute("plantTotalCount", catalogPlants.size());
        model.addAttribute("plantVisibleCount", catalogPlants.stream().filter(plant -> plant.deletedAt() == null).count());
        model.addAttribute("plantHiddenCount", catalogPlants.stream().filter(plant -> plant.deletedAt() != null).count());
        model.addAttribute("plantWithoutVariantCount", catalogPlants.stream()
                .filter(plant -> plantVariantCounts.getOrDefault(plant.plantId(), 0L) == 0L)
                .count());
        model.addAttribute("plantAirPurifyingCount", catalogPlants.stream().filter(plant -> Boolean.TRUE.equals(plant.airPurifying())).count());
        model.addAttribute("variantTotalCount", variants.size());
        model.addAttribute("variantActiveCount", variants.stream().filter(variant -> Boolean.TRUE.equals(variant.isActive())).count());
        model.addAttribute("variantHiddenCount", variants.stream().filter(variant -> !Boolean.TRUE.equals(variant.isActive())).count());
        model.addAttribute("variantOutOfStockCount", variants.stream()
                .filter(variant -> variant.quantity() == null || variant.quantity() <= 0)
                .count());
        model.addAttribute("variantLowStockCount", variants.stream()
                .filter(variant -> variant.quantity() != null && variant.quantity() > 0 && variant.quantity() <= 5)
                .count());
        model.addAttribute("variantTotalStock", variants.stream()
                .map(ProductVariantResponse::quantity)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum());
        model.addAttribute("careProfiles", careProfiles);
        model.addAttribute("careProfileTotalCount", careProfiles.size());
        model.addAttribute("carePlantCoveredCount", carePlantIds.size());
        model.addAttribute("carePlantMissingCount", plants.stream()
                .filter(plant -> plant.plantId() != null && !carePlantIds.contains(plant.plantId()))
                .count());
        model.addAttribute("careEasyCount", careProfiles.stream().filter(profile -> profile.careLevel() != null && profile.careLevel() == 1).count());
        model.addAttribute("careNeedsExperienceCount", careProfiles.stream().filter(profile -> profile.careLevel() != null && profile.careLevel() >= 3).count());
        model.addAttribute("inventoryItems", inventoryItems);
        model.addAttribute("inventoryTrackedCount", inventoryItems.stream().filter(item -> Boolean.TRUE.equals(item.isActive())).count());
        model.addAttribute("inventoryTotalStock", inventoryItems.stream()
                .map(ProductVariantResponse::quantity)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum());
        model.addAttribute("inventoryLowStockCount", inventoryItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.isActive()) && item.quantity() != null && item.quantity() > 0 && item.quantity() <= 5)
                .count());
        model.addAttribute("inventoryOutOfStockCount", inventoryItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.isActive()) && (item.quantity() == null || item.quantity() <= 0))
                .count());
        model.addAttribute("inventoryInStockCount", inventoryItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.isActive()) && item.quantity() != null && item.quantity() > 5)
                .count());
        model.addAttribute("inventoryStoppedCount", inventoryItems.stream().filter(item -> !Boolean.TRUE.equals(item.isActive())).count());
        model.addAttribute("coupons", coupons);
        model.addAttribute("regularCoupons", regularCoupons);
        model.addAttribute("systemCoupons", systemCoupons);
        model.addAttribute("couponTotalUsage", regularCoupons.stream()
                .map(CouponResponse::usedCount)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum());
        model.addAttribute("couponActiveCount", regularCouponViewState.activeCount());
        model.addAttribute("couponExpiringCount", regularCouponViewState.expiringCount());
        model.addAttribute("couponExpiredCount", regularCouponViewState.expiredCount());
        model.addAttribute("couponUsedOutCount", regularCouponViewState.usedOutCount());
        model.addAttribute("couponStatusKeys", couponViewState.statusKeys());
        model.addAttribute("couponStatusLabels", couponViewState.statusLabels());
        model.addAttribute("couponUsedOutFlags", couponViewState.usedOutFlags());
        model.addAttribute("couponSystemCodeFlags", couponViewState.systemCodeFlags());
        model.addAttribute("couponDeactivateDisabledFlags", couponViewState.deactivateDisabledFlags());
        model.addAttribute("now", now);
        model.addAttribute("soon", soon);
        model.addAttribute("articles", articles);
        model.addAttribute("articleTotalCount", articles.size());
        model.addAttribute("articleMissingImageCount", articles.stream()
                .filter(article -> article.thumbnail() == null || article.thumbnail().isBlank())
                .count());
        model.addAttribute("orders", orders);
        model.addAttribute("notifications", notifications);
        model.addAttribute("notificationUsers", notificationService.getActiveUsers());
        model.addAttribute("notificationTotalCount", notifications.size());
        model.addAttribute("notificationSentTodayCount", notifications.stream()
                .filter(notification -> notification.createdAt() != null && notification.createdAt().toLocalDate().equals(now.toLocalDate()))
                .count());
        model.addAttribute("notificationSystemCount", notifications.stream().filter(notification -> notification.type() == null || notification.type() == 0).count());
        model.addAttribute("notificationOrderCount", notifications.stream().filter(notification -> notification.type() != null && notification.type() == 1).count());
        model.addAttribute("notificationPromotionCount", notifications.stream().filter(notification -> notification.type() != null && notification.type() == 2).count());
        model.addAttribute("notificationLatestRecipientCount", notifications.stream()
                .findFirst()
                .map(AdminNotificationResponse::recipientCount)
                .orElse(0));
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewTotalCount", reviews.size());
        model.addAttribute("reviewPendingCount", reviews.stream().filter(review -> review.isApproved() == null).count());
        model.addAttribute("reviewApprovedCount", reviews.stream().filter(review -> Boolean.TRUE.equals(review.isApproved())).count());
        model.addAttribute("reviewHiddenCount", reviews.stream().filter(review -> Boolean.FALSE.equals(review.isApproved())).count());
        model.addAttribute("reviewLowRatingCount", reviews.stream().filter(review -> review.rating() != null && review.rating() <= 2).count());
        model.addAttribute("reviewAverageRating", reviews.stream()
                .map(AdminReviewResponse::rating)
                .filter(rating -> rating != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        model.addAttribute("productStats", analyticsService.getProductStatistics());
        model.addAttribute("revenueDashboard", analyticsService.getRevenueDashboard(
                normalizedDashboardDays,
                revenueRange,
                revenueStart,
                revenueEnd
        ));
        model.addAttribute("dashboardDays", normalizedDashboardDays);
        model.addAttribute("users", users);
        model.addAttribute("userTotalCount", users.size());
        model.addAttribute("userAdminCount", users.stream().filter(user -> user.role() != null && user.role() == 0).count());
        model.addAttribute("userCustomerCount", users.stream().filter(user -> user.role() == null || user.role() == 1).count());
        model.addAttribute("userActiveCount", users.stream().filter(user -> user.status() != null && user.status() == 1).count());
        model.addAttribute("userLockedCount", users.stream().filter(user -> user.status() != null && (user.status() == 2 || user.status() == -1)).count());
        model.addAttribute("userNewSevenDaysCount", users.stream()
                .filter(user -> user.createdAt() != null && !user.createdAt().isBefore(sevenDaysAgo))
                .count());
        return "admin/catalog";
    }

    @PostMapping("/admin/categories")
    public String createCategory(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) Integer sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        catalogService.createCategory(new CategoryRequest(title, description, imageUrl, isActive, sortOrder));
        return success(redirectAttributes, "Đã thêm danh mục mới.", "categories");
    }

    @PostMapping("/admin/categories/{categoryId}")
    public String updateCategory(
            @PathVariable String categoryId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) Integer sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        catalogService.updateCategory(categoryId, new CategoryRequest(title, description, imageUrl, isActive, sortOrder));
        return success(redirectAttributes, "Đã cập nhật danh mục.", "categories");
    }

    @PostMapping("/admin/categories/{categoryId}/visibility")
    public String setCategoryVisibility(
            @PathVariable String categoryId,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            RedirectAttributes redirectAttributes
    ) {
        catalogService.setCategoryActive(categoryId, Boolean.TRUE.equals(isActive));
        return success(
                redirectAttributes,
                Boolean.TRUE.equals(isActive) ? "Đã hiển thị danh mục." : "Đã ẩn danh mục.",
                "categories"
        );
    }

    @PostMapping("/admin/categories/{categoryId}/deactivate")
    public String deactivateCategory(@PathVariable String categoryId, RedirectAttributes redirectAttributes) {
        catalogService.deactivateCategory(categoryId);
        return success(redirectAttributes, "Đã ẩn danh mục.", "categories");
    }

    @PostMapping("/admin/categories/{categoryId}/delete")
    public String deleteCategory(@PathVariable String categoryId, RedirectAttributes redirectAttributes) {
        try {
            catalogService.deleteCategoryIfUnused(categoryId);
            return success(redirectAttributes, "Đã xóa danh mục.", "categories");
        } catch (IllegalArgumentException exception) {
            return warning(redirectAttributes, exception.getMessage(), "categories");
        }
    }

    @PostMapping("/admin/plants")
    public String createPlant(
            @RequestParam String title,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String scientificName,
            @RequestParam(required = false) String commonName,
            @RequestParam(required = false) Integer plantType,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String toxicity,
            @RequestParam(required = false, defaultValue = "false") Boolean petFriendly,
            @RequestParam(required = false, defaultValue = "false") Boolean airPurifying,
            @RequestParam(required = false) String categoryId,
            RedirectAttributes redirectAttributes
    ) {
        plantService.createPlant(new PlantRequest(
                title, sku, description, scientificName, commonName, plantType,
                origin, toxicity, petFriendly, airPurifying, categoryId
        ));
        return success(redirectAttributes, "Đã thêm cây mới.", "plants");
    }

    @PostMapping("/admin/plants/{plantId}")
    public String updatePlant(
            @PathVariable String plantId,
            @RequestParam String title,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String scientificName,
            @RequestParam(required = false) String commonName,
            @RequestParam(required = false) Integer plantType,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String toxicity,
            @RequestParam(required = false, defaultValue = "false") Boolean petFriendly,
            @RequestParam(required = false, defaultValue = "false") Boolean airPurifying,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false, defaultValue = "true") Boolean isVisible,
            RedirectAttributes redirectAttributes
    ) {
        plantService.updatePlant(plantId, new PlantRequest(
                title, sku, description, scientificName, commonName, plantType,
                origin, toxicity, petFriendly, airPurifying, categoryId
        ));
        if (Boolean.TRUE.equals(isVisible)) {
            plantService.restorePlant(plantId);
        } else {
            plantService.softDeletePlant(plantId);
        }
        return success(redirectAttributes, "Đã cập nhật cây.", "plants");
    }

    @PostMapping("/admin/plants/{plantId}/visibility")
    public String setPlantVisibility(
            @PathVariable String plantId,
            @RequestParam(required = false, defaultValue = "true") Boolean isVisible,
            RedirectAttributes redirectAttributes
    ) {
        if (Boolean.TRUE.equals(isVisible)) {
            plantService.restorePlant(plantId);
            return success(redirectAttributes, "Đã hiển thị cây.", "plants");
        }
        plantService.softDeletePlant(plantId);
        return success(redirectAttributes, "Đã ẩn cây.", "plants");
    }

    @PostMapping("/admin/plants/{plantId}/delete")
    public String deletePlant(@PathVariable String plantId, RedirectAttributes redirectAttributes) {
        plantService.softDeletePlant(plantId);
        return success(redirectAttributes, "Đã ẩn cây để bảo toàn dữ liệu liên kết.", "plants");
    }

    @PostMapping("/admin/variants")
    public String createVariant(
            @RequestParam String plantId,
            @RequestParam String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer heightCm,
            @RequestParam(required = false) Integer potSize,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal salePrice,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) String seoDescription,
            @RequestParam(required = false) String seoTitle,
            RedirectAttributes redirectAttributes
    ) {
        variantService.createVariant(new ProductVariantRequest(
                plantId, name, sku, heightCm, potSize, price, salePrice,
                quantity, attribute, isActive, seoDescription, seoTitle
        ));
        return success(redirectAttributes, "Đã thêm biến thể mới.", "variants");
    }

    @PostMapping("/admin/variants/{variantId}")
    public String updateVariant(
            @PathVariable String variantId,
            @RequestParam String plantId,
            @RequestParam String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer heightCm,
            @RequestParam(required = false) Integer potSize,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal salePrice,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            @RequestParam(required = false) String seoDescription,
            @RequestParam(required = false) String seoTitle,
            RedirectAttributes redirectAttributes
    ) {
        variantService.updateVariant(variantId, new ProductVariantRequest(
                plantId, name, sku, heightCm, potSize, price, salePrice,
                quantity, attribute, isActive, seoDescription, seoTitle
        ));
        return success(redirectAttributes, "Đã cập nhật biến thể.", "variants");
    }

    @PostMapping("/admin/variants/{variantId}/visibility")
    public String setVariantVisibility(
            @PathVariable String variantId,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            RedirectAttributes redirectAttributes
    ) {
        variantService.setVariantActive(variantId, Boolean.TRUE.equals(isActive));
        return success(
                redirectAttributes,
                Boolean.TRUE.equals(isActive) ? "Đã hiển thị biến thể." : "Đã ẩn biến thể.",
                "variants"
        );
    }

    @PostMapping("/admin/variants/{variantId}/deactivate")
    public String deactivateVariant(@PathVariable String variantId, RedirectAttributes redirectAttributes) {
        variantService.deactivateVariant(variantId);
        return success(redirectAttributes, "Đã ẩn biến thể.", "variants");
    }

    @PostMapping("/admin/care-profiles")
    public String saveCareProfile(
            @RequestParam String plantId,
            @RequestParam(required = false) String lightRequirement,
            @RequestParam(required = false) String wateringFrequency,
            @RequestParam(required = false) String humidityRequirement,
            @RequestParam(required = false) Integer careLevel,
            @RequestParam(required = false) String careInstruction,
            RedirectAttributes redirectAttributes
    ) {
        careService.saveCareProfile(
                plantId,
                lightRequirement,
                wateringFrequency,
                humidityRequirement,
                careLevel,
                careInstruction
        );
        return success(redirectAttributes, "Đã cập nhật hồ sơ chăm sóc cây.", "care");
    }

    @PostMapping("/admin/inventory/stock")
    public String updateStock(
            @RequestParam String variantId,
            @RequestParam String movementType,
            @RequestParam Integer amount,
            RedirectAttributes redirectAttributes
    ) {
        inventoryService.updateStock(variantId, movementType, amount);
        return success(redirectAttributes, "Đã cập nhật tồn kho.", "inventory");
    }

    @PostMapping("/admin/inventory/{variantId}/visibility")
    public String setInventoryVisibility(
            @PathVariable String variantId,
            @RequestParam(required = false, defaultValue = "false") Boolean isActive,
            RedirectAttributes redirectAttributes
    ) {
        inventoryService.setTracking(variantId, Boolean.TRUE.equals(isActive));
        return success(
                redirectAttributes,
                Boolean.TRUE.equals(isActive) ? "Da theo doi lai ton kho." : "Da ngung theo doi ton kho.",
                "inventory"
        );
    }

    @PostMapping("/admin/care-profiles/{careId}/delete")
    public String deleteCareProfile(@PathVariable String careId, RedirectAttributes redirectAttributes) {
        careService.deleteCareProfile(careId);
        return success(redirectAttributes, "Da xoa ho so cham soc cay.", "care");
    }

    @PostMapping("/admin/orders/{orderId}/status")
    public String updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String estimatedDelivery,
            RedirectAttributes redirectAttributes
    ) {
        orderService.updateOrderStatus(orderId, new OrderStatusUpdateRequest(status, paymentStatus, note, estimatedDelivery));
        return success(redirectAttributes, "Đã cập nhật trạng thái đơn hàng.");
    }

    @PostMapping("/admin/orders/{orderId}/payment")
    public String updateOrderPayment(
            @PathVariable String orderId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String gatewayResponse,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidAt,
            RedirectAttributes redirectAttributes
    ) {
        orderService.updatePayment(orderId, new PaymentUpdateRequest(transactionId, amount, method, status, gatewayResponse, paidAt));
        return success(redirectAttributes, "Đã cập nhật thanh toán.");
    }

    @PostMapping("/admin/notifications")
    public String sendNotification(
            @RequestParam String targetType,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) Integer type,
            @RequestParam String title,
            @RequestParam String messageText,
            @RequestParam(required = false) String data,
            RedirectAttributes redirectAttributes
    ) {
        notificationService.sendNotification(new AdminNotificationRequest(
                targetType, role, userId, userEmail, type, title, messageText, data
        ));
        return success(redirectAttributes, "Đã gửi thông báo.", "notifications");
    }

    @PostMapping("/admin/notifications/{notificationId}/delete")
    public String deleteNotification(@PathVariable String notificationId, RedirectAttributes redirectAttributes) {
        notificationService.deleteNotification(notificationId);
        return success(redirectAttributes, "Da xoa thong bao.", "notifications");
    }

    @PostMapping("/admin/reviews/{reviewId}/moderation")
    public String moderateReview(
            @PathVariable String reviewId,
            @RequestParam(required = false) Boolean isApproved,
            @RequestParam(required = false) String replyMessage,
            RedirectAttributes redirectAttributes
    ) {
        reviewService.moderateReview(reviewId, new ReviewModerationRequest(isApproved, replyMessage));
        return success(redirectAttributes, "Đã cập nhật đánh giá.", "reviews");
    }

    @PostMapping("/admin/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable String reviewId, RedirectAttributes redirectAttributes) {
        reviewService.deleteReview(reviewId);
        return success(redirectAttributes, "Da xoa danh gia.", "reviews");
    }

    @PostMapping("/admin/users/{userId}")
    public String updateUser(
            @AuthenticationPrincipal(expression = "user") UserEntity currentAdmin,
            @PathVariable String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status,
            RedirectAttributes redirectAttributes
    ) {
        AdminUserResponse before = userService.getUser(userId);
        AdminUserResponse after = userService.updateUser(userId, new AdminUserUpdateRequest(title, phone, role, status), currentAdmin);
        return success(redirectAttributes, userUpdateMessage(before, after), "users");
    }

    @PostMapping("/admin/users/{userId}/delete")
    public String deleteUser(
            @AuthenticationPrincipal(expression = "user") UserEntity currentAdmin,
            @PathVariable String userId,
            RedirectAttributes redirectAttributes
    ) {
        userService.deleteUser(userId, currentAdmin);
        return success(redirectAttributes, "Da xoa user.", "users");
    }

    @PostMapping("/admin/coupons")
    public String createCoupon(
            @RequestParam String code,
            @RequestParam Integer type,
            @RequestParam BigDecimal value,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) Integer maxUses,
            @RequestParam(required = false) Integer perUserLimit,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            RedirectAttributes redirectAttributes
    ) {
        couponService.createCoupon(new CouponRequest(
                code, type, value, minOrderAmount, maxDiscountAmount,
                maxUses, perUserLimit, isActive, startsAt, expiresAt
        ));
        return success(redirectAttributes, "Đã thêm mã giảm giá.");
    }

    @PostMapping("/admin/coupons/{couponId}")
    public String updateCoupon(
            @PathVariable String couponId,
            @RequestParam String code,
            @RequestParam Integer type,
            @RequestParam BigDecimal value,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) Integer maxUses,
            @RequestParam(required = false) Integer perUserLimit,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            RedirectAttributes redirectAttributes
    ) {
        couponService.updateCoupon(couponId, new CouponRequest(
                code, type, value, minOrderAmount, maxDiscountAmount,
                maxUses, perUserLimit, isActive, startsAt, expiresAt
        ));
        return success(redirectAttributes, "Đã cập nhật mã giảm giá.");
    }

    @PostMapping("/admin/coupons/{couponId}/deactivate")
    public String deactivateCoupon(@PathVariable String couponId, RedirectAttributes redirectAttributes) {
        couponService.deactivateCoupon(couponId);
        return success(redirectAttributes, "Đã ẩn mã giảm giá.");
    }

    @PostMapping("/admin/coupons/{couponId}/delete")
    public String deleteCoupon(@PathVariable String couponId, RedirectAttributes redirectAttributes) {
        couponService.deleteCoupon(couponId);
        return success(redirectAttributes, "Đã xóa mã giảm giá.");
    }

    @PostMapping("/admin/articles")
    public String createArticle(
            @AuthenticationPrincipal(expression = "user") UserEntity author,
            @RequestParam String title,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String excerpt,
            @RequestParam String content,
            @RequestParam(required = false) String thumbnail,
            RedirectAttributes redirectAttributes
    ) {
        articleService.createArticle(new PlantCareArticleRequest(title, slug, excerpt, content, thumbnail), author);
        return success(redirectAttributes, "Đã thêm bài viết.", "articles");
    }

    @PostMapping("/admin/articles/{articleId}")
    public String updateArticle(
            @PathVariable String articleId,
            @RequestParam String title,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String excerpt,
            @RequestParam String content,
            @RequestParam(required = false) String thumbnail,
            RedirectAttributes redirectAttributes
    ) {
        articleService.updateArticle(articleId, new PlantCareArticleRequest(title, slug, excerpt, content, thumbnail));
        return success(redirectAttributes, "Đã cập nhật bài viết.", "articles");
    }

    @PostMapping("/admin/articles/{articleId}/delete")
    public String deleteArticle(@PathVariable String articleId, RedirectAttributes redirectAttributes) {
        articleService.deleteArticle(articleId);
        return success(redirectAttributes, "Đã xóa bài viết.", "articles");
    }

    private String success(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("success", message);
        return "redirect:/admin";
    }

    private String success(RedirectAttributes redirectAttributes, String message, String section) {
        redirectAttributes.addFlashAttribute("success", message);
        return "redirect:/admin#" + section;
    }

    private String warning(RedirectAttributes redirectAttributes, String message, String section) {
        redirectAttributes.addFlashAttribute("warning", message);
        return "redirect:/admin#" + section;
    }

    private String userUpdateMessage(AdminUserResponse before, AdminUserResponse after) {
        String userLabel = after.title() == null || after.title().isBlank()
                ? (after.email() == null || after.email().isBlank() ? "user" : after.email())
                : after.title();
        if (!Integer.valueOf(0).equals(before.role()) && Integer.valueOf(0).equals(after.role())) {
            return "Đã nâng quyền " + userLabel + " lên Admin. Tài khoản này có quyền truy cập khu vực quản trị.";
        }
        if (Integer.valueOf(0).equals(before.role()) && Integer.valueOf(1).equals(after.role())) {
            return "Đã hạ quyền " + userLabel + " xuống User. Tài khoản không còn quyền truy cập khu vực quản trị.";
        }
        if (!Integer.valueOf(2).equals(before.status()) && Integer.valueOf(2).equals(after.status())) {
            return "Đã khóa tài khoản " + userLabel + ". Token đăng nhập hiện tại đã bị thu hồi.";
        }
        if (Integer.valueOf(2).equals(before.status()) && Integer.valueOf(1).equals(after.status())) {
            return "Đã mở khóa tài khoản " + userLabel + ".";
        }
        return "Đã cập nhật user " + userLabel + ".";
    }

    private int normalizeDashboardDays(Integer dashboardDays) {
        if (dashboardDays == null) {
            return 14;
        }
        return switch (dashboardDays) {
            case 7, 14, 30 -> dashboardDays;
            default -> 14;
        };
    }

    private CouponViewState buildCouponViewState(List<CouponResponse> coupons, LocalDateTime now, LocalDateTime soon) {
        Map<String, String> statusKeys = new HashMap<>();
        Map<String, String> statusLabels = new HashMap<>();
        Map<String, Boolean> usedOutFlags = new HashMap<>();
        Map<String, Boolean> systemCodeFlags = new HashMap<>();
        Map<String, Boolean> deactivateDisabledFlags = new HashMap<>();
        int activeCount = 0;
        int expiringCount = 0;
        int expiredCount = 0;
        int usedOutCount = 0;

        for (CouponResponse coupon : coupons) {
            boolean usedOut = isUsedOut(coupon);
            boolean expired = coupon.expiresAt() != null && coupon.expiresAt().isBefore(now);
            boolean upcoming = coupon.startsAt() != null && coupon.startsAt().isAfter(now);
            boolean expiring = coupon.expiresAt() != null
                    && coupon.expiresAt().isAfter(now)
                    && coupon.expiresAt().isBefore(soon);
            String statusKey = couponStatusKey(coupon, usedOut, expired, upcoming, expiring);
            String couponId = coupon.couponsId();

            statusKeys.put(couponId, statusKey);
            statusLabels.put(couponId, couponStatusLabel(statusKey));
            usedOutFlags.put(couponId, usedOut);
            systemCodeFlags.put(couponId, isSystemCoupon(coupon));
            deactivateDisabledFlags.put(couponId, isSystemCoupon(coupon) || !Boolean.TRUE.equals(coupon.isActive()));

            if ("active".equals(statusKey)) {
                activeCount++;
            }
            if ("expiring".equals(statusKey)) {
                expiringCount++;
            }
            if ("expired".equals(statusKey)) {
                expiredCount++;
            }
            if (usedOut) {
                usedOutCount++;
            }
        }

        return new CouponViewState(
                activeCount,
                expiringCount,
                expiredCount,
                usedOutCount,
                statusKeys,
                statusLabels,
                usedOutFlags,
                systemCodeFlags,
                deactivateDisabledFlags
        );
    }

    private boolean isUsedOut(CouponResponse coupon) {
        return coupon.maxUses() != null
                && coupon.usedCount() != null
                && coupon.usedCount() >= coupon.maxUses();
    }

    private boolean isSystemCoupon(CouponResponse coupon) {
        return coupon != null && "NO_COUPON".equalsIgnoreCase(coupon.code());
    }

    private String couponStatusKey(CouponResponse coupon, boolean usedOut, boolean expired, boolean upcoming, boolean expiring) {
        if (!Boolean.TRUE.equals(coupon.isActive())) {
            return "inactive";
        }
        if (expired) {
            return "expired";
        }
        if (usedOut) {
            return "used-out";
        }
        if (upcoming) {
            return "upcoming";
        }
        if (expiring) {
            return "expiring";
        }
        return "active";
    }

    private String couponStatusLabel(String statusKey) {
        return switch (statusKey) {
            case "inactive" -> "Tạm ẩn";
            case "expired" -> "Hết hạn";
            case "used-out" -> "Đã dùng hết lượt";
            case "upcoming" -> "Chưa bắt đầu";
            case "expiring" -> "Sắp hết hạn";
            default -> "Đang hoạt động";
        };
    }

    private record CouponViewState(
            int activeCount,
            int expiringCount,
            int expiredCount,
            int usedOutCount,
            Map<String, String> statusKeys,
            Map<String, String> statusLabels,
            Map<String, Boolean> usedOutFlags,
            Map<String, Boolean> systemCodeFlags,
            Map<String, Boolean> deactivateDisabledFlags
    ) {
    }
}
