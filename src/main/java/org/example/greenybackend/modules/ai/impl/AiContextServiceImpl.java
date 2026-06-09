package org.example.greenybackend.modules.ai.impl;

import org.example.greenybackend.modules.ai.AiContextService;
import org.example.greenybackend.modules.ai.AiContextResult;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.example.greenybackend.domain.entity.Category;
import org.example.greenybackend.domain.entity.Coupons;
import org.example.greenybackend.domain.entity.OrderItems;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.PlantCareProfile;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.category.CategoryRepository;
import org.example.greenybackend.modules.order.OrdersRepository;
import org.example.greenybackend.modules.plant.PlantCareProfileRepository;
import org.example.greenybackend.modules.promotion.CouponsRepository;
import org.example.greenybackend.modules.variant.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiContextServiceImpl implements AiContextService {

    private static final int MAX_PRODUCTS = 8;
    private static final int MAX_CATEGORIES = 8;
    private static final int MAX_CARE = 8;
    private static final int MAX_COUPONS = 5;
    private static final int MAX_ORDERS = 5;
    private static final List<String> AI_ENTITIES = List.of(
            "Plant",
            "Category",
            "ProductVariant",
            "PlantCareProfile",
            "Coupons"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "minh", "ban", "cho", "hoi", "ve", "la", "co", "khong", "nao", "nhung",
            "cac", "mot", "cua", "trong", "ngoai", "can", "muon", "mua", "san", "pham", "cay",
            "hay", "biet", "tat", "ca", "thong", "tin", "toan", "bo", "chi", "tiet"
    );

    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final PlantCareProfileRepository careProfileRepository;
    private final CouponsRepository couponsRepository;
    private final OrdersRepository ordersRepository;

    public AiContextServiceImpl(
            ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository,
            PlantCareProfileRepository careProfileRepository,
            CouponsRepository couponsRepository,
            OrdersRepository ordersRepository
    ) {
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.careProfileRepository = careProfileRepository;
        this.couponsRepository = couponsRepository;
        this.ordersRepository = ordersRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public AiContextResult buildContext(UserEntity user, String question) {
        String normalizedQuestion = normalize(question);
        String intent = detectIntent(normalizedQuestion);
        if ("OUT_OF_SCOPE".equals(intent)) {
            return new AiContextResult(intent, AI_ENTITIES, 0, "", List.of());
        }

        List<String> terms = searchTerms(normalizedQuestion);
        List<ProductVariant> variants = findRelevantVariants(intent, terms);
        List<Category> categories = findRelevantCategories(intent, terms);
        List<PlantCareProfile> careProfiles = findRelevantCareProfiles(intent, terms);
        List<Coupons> coupons = findRelevantCoupons(intent, terms);
        List<Orders> orders = findRelevantOrders(user, intent, normalizedQuestion);

        StringBuilder context = new StringBuilder();
        List<String> summary = new ArrayList<>();
        appendProductContext(context, summary, variants);
        appendCategoryContext(context, summary, categories);
        appendCareContext(context, summary, careProfiles);
        appendCouponContext(context, summary, coupons);
        appendOrderContext(context, summary, orders);

        int totalRecords = variants.size() + categories.size() + careProfiles.size() + coupons.size() + orders.size();
        return new AiContextResult(intent, AI_ENTITIES, totalRecords, context.toString().trim(), summary);
    }

    private String detectIntent(String normalizedQuestion) {
        if (normalizedQuestion == null || normalizedQuestion.isBlank()) {
            return "OUT_OF_SCOPE";
        }
        if (containsAny(normalizedQuestion, "don hang", "order", "van chuyen", "giao hang", "thanh toan", "huy don")) {
            return "ORDER";
        }
        if (containsAny(normalizedQuestion, "khuyen mai", "giam gia", "coupon", "voucher", "ma giam", "uu dai")) {
            return "PRICE_PROMOTION";
        }
        if (containsAny(normalizedQuestion, "gia", "bao nhieu", "tien", "re", "dat")) {
            return "PRICE_PROMOTION";
        }
        if (containsAny(normalizedQuestion, "bien the", "variant", "sku", "kich thuoc", "chieu cao", "chau", "ton kho", "con hang")) {
            return "VARIANT";
        }
        if (containsAny(normalizedQuestion, "cham soc", "tuoi", "nuoc", "anh sang", "do am", "dat", "la vang", "sau", "benh")) {
            return "CARE";
        }
        if (containsAny(normalizedQuestion, "loai", "danh muc", "category", "trong nha", "ngoai troi")) {
            return "CATEGORY";
        }
        if (containsAny(normalizedQuestion, "cay", "plant", "san pham", "mua", "shop", "greeny")) {
            return "PRODUCT";
        }
        return "OUT_OF_SCOPE";
    }

    private List<ProductVariant> findRelevantVariants(String intent, List<String> terms) {
        boolean productIntent = List.of("PRODUCT", "VARIANT", "PRICE_PROMOTION", "CARE").contains(intent);
        if (!productIntent) {
            return List.of();
        }
        List<String> effectiveTerms = "PRICE_PROMOTION".equals(intent) ? nonGenericPriceTerms(terms) : terms;
        List<ProductVariant> variants = variantRepository.findByIsActiveTrue().stream()
                .filter(variant -> variant.getPlant() != null)
                .filter(variant -> variant.getPlant().getDeletedAt() == null)
                .filter(variant -> effectiveTerms.isEmpty() || scoreVariant(variant, effectiveTerms) > 0)
                .sorted(variantComparator(intent, effectiveTerms))
                .limit(MAX_PRODUCTS)
                .toList();
        if (variants.isEmpty() && effectiveTerms.isEmpty()) {
            return variantRepository.findByIsActiveTrue().stream()
                    .filter(variant -> variant.getPlant() != null && variant.getPlant().getDeletedAt() == null)
                    .limit(MAX_PRODUCTS)
                    .toList();
        }
        return variants;
    }

    private List<Category> findRelevantCategories(String intent, List<String> terms) {
        if (!List.of("CATEGORY", "PRODUCT").contains(intent)) {
            return List.of();
        }
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAscTitleAsc().stream()
                .filter(category -> terms.isEmpty() || scoreText(terms, category.getTitle(), category.getDescription()) > 0)
                .limit(MAX_CATEGORIES)
                .toList();
    }

    private List<PlantCareProfile> findRelevantCareProfiles(String intent, List<String> terms) {
        if (!List.of("CARE", "PRODUCT").contains(intent)) {
            return List.of();
        }
        return careProfileRepository.findAll().stream()
                .filter(profile -> profile.getPlant() != null && profile.getPlant().getDeletedAt() == null)
                .filter(profile -> terms.isEmpty() || scoreCare(profile, terms) > 0)
                .limit(MAX_CARE)
                .toList();
    }

    private List<Coupons> findRelevantCoupons(String intent, List<String> terms) {
        if (!"PRICE_PROMOTION".equals(intent)) {
            return List.of();
        }
        List<String> effectiveTerms = nonGenericPriceTerms(terms);
        LocalDateTime now = LocalDateTime.now();
        return couponsRepository.findAll().stream()
                .filter(coupon -> Boolean.TRUE.equals(coupon.getIsActive()))
                .filter(coupon -> coupon.getStartsAt() == null || !coupon.getStartsAt().isAfter(now))
                .filter(coupon -> coupon.getExpiresAt() == null || !coupon.getExpiresAt().isBefore(now))
                .filter(coupon -> effectiveTerms.isEmpty()
                        || scoreText(effectiveTerms, coupon.getCode()) > 0
                        || containsAny(normalize(coupon.getCode()), effectiveTerms))
                .limit(MAX_COUPONS)
                .toList();
    }

    private List<Orders> findRelevantOrders(UserEntity user, String intent, String normalizedQuestion) {
        if (!"ORDER".equals(intent) || user == null || user.getUserId() == null) {
            return List.of();
        }
        return ordersRepository.findByUserEntityUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .filter(order -> normalizedQuestion == null
                        || normalizedQuestion.isBlank()
                        || order.getOrderId() == null
                        || normalizedQuestion.contains(normalize(order.getOrderId()).substring(0, Math.min(8, order.getOrderId().length())))
                        || !hasOrderIdLikeText(normalizedQuestion))
                .limit(MAX_ORDERS)
                .toList();
    }

    private Comparator<ProductVariant> variantComparator(String intent, List<String> terms) {
        if ("PRICE_PROMOTION".equals(intent)) {
            return Comparator.comparing(this::effectivePrice, Comparator.nullsLast(BigDecimal::compareTo));
        }
        return Comparator
                .comparing((ProductVariant variant) -> scoreVariant(variant, terms))
                .reversed()
                .thenComparing(ProductVariant::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int scoreVariant(ProductVariant variant, List<String> terms) {
        Plant plant = variant.getPlant();
        Category category = plant == null ? null : plant.getCategory();
        return scoreText(
                terms,
                plant == null ? null : plant.getTitle(),
                plant == null ? null : plant.getDescription(),
                plant == null ? null : plant.getScientificName(),
                plant == null ? null : plant.getCommonName(),
                plant == null ? null : plant.getOrigin(),
                plant == null ? null : plant.getToxicity(),
                category == null ? null : category.getTitle(),
                variant.getName(),
                variant.getSku(),
                variant.getAttribute(),
                variant.getSeoTitle(),
                variant.getSeoDescription()
        );
    }

    private int scoreCare(PlantCareProfile profile, List<String> terms) {
        Plant plant = profile.getPlant();
        return scoreText(
                terms,
                plant == null ? null : plant.getTitle(),
                plant == null ? null : plant.getCommonName(),
                plant == null ? null : plant.getScientificName(),
                profile.getLightRequirement(),
                profile.getWateringFrequency(),
                profile.getHumidityRequirement(),
                profile.getCareInstruction()
        );
    }

    private int scoreText(List<String> terms, String... values) {
        int score = 0;
        for (String term : terms) {
            for (String value : values) {
                if (value != null && normalize(value).contains(term)) {
                    score++;
                }
            }
        }
        return score;
    }

    private void appendProductContext(StringBuilder context, List<String> summary, List<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return;
        }
        context.append("Products:\n");
        for (ProductVariant variant : variants) {
            Plant plant = variant.getPlant();
            Category category = plant == null ? null : plant.getCategory();
            context.append("- id: ").append(safe(plant == null ? null : plant.getPlantId())).append("\n");
            context.append("  name: ").append(safe(plant == null ? null : plant.getTitle())).append("\n");
            context.append("  description: ").append(shortText(plant == null ? null : plant.getDescription(), 260)).append("\n");
            context.append("  scientificName: ").append(safe(plant == null ? null : plant.getScientificName())).append("\n");
            context.append("  commonName: ").append(safe(plant == null ? null : plant.getCommonName())).append("\n");
            context.append("  category: ").append(safe(category == null ? null : category.getTitle())).append("\n");
            context.append("  toxicity: ").append(safe(plant == null ? null : plant.getToxicity())).append("\n");
            context.append("  petFriendly: ").append(plant == null ? null : plant.getPetFriendly()).append("\n");
            context.append("  airPurifying: ").append(plant == null ? null : plant.getAirPurifying()).append("\n");
            context.append("  variant:\n");
            context.append("    id: ").append(safe(variant.getVariantId())).append("\n");
            context.append("    name: ").append(safe(variant.getName())).append("\n");
            context.append("    sku: ").append(safe(variant.getSku())).append("\n");
            context.append("    heightCm: ").append(variant.getHeightCm()).append("\n");
            context.append("    potSize: ").append(variant.getPotSize()).append("\n");
            context.append("    price: ").append(variant.getPrice()).append("\n");
            context.append("    salePrice: ").append(variant.getSalePrice()).append("\n");
            context.append("    effectivePrice: ").append(effectivePrice(variant)).append("\n");
            context.append("    quantity: ").append(variant.getQuantity()).append("\n");
            context.append("    attribute: ").append(safe(variant.getAttribute())).append("\n");
            summary.add("Product " + safe(plant == null ? null : plant.getTitle()) + " - " + safe(variant.getName()) + " - " + effectivePrice(variant));
        }
    }

    private void appendCategoryContext(StringBuilder context, List<String> summary, List<Category> categories) {
        if (categories.isEmpty()) {
            return;
        }
        context.append("Categories:\n");
        for (Category category : categories) {
            context.append("- id: ").append(safe(category.getCaId())).append("\n");
            context.append("  title: ").append(safe(category.getTitle())).append("\n");
            context.append("  description: ").append(shortText(category.getDescription(), 220)).append("\n");
            context.append("  sortOrder: ").append(category.getSortOrder()).append("\n");
            summary.add("Category " + safe(category.getTitle()));
        }
    }

    private void appendCareContext(StringBuilder context, List<String> summary, List<PlantCareProfile> profiles) {
        if (profiles.isEmpty()) {
            return;
        }
        context.append("CareProfiles:\n");
        for (PlantCareProfile profile : profiles) {
            Plant plant = profile.getPlant();
            context.append("- id: ").append(safe(profile.getCareId())).append("\n");
            context.append("  plantId: ").append(safe(plant == null ? null : plant.getPlantId())).append("\n");
            context.append("  plantName: ").append(safe(plant == null ? null : plant.getTitle())).append("\n");
            context.append("  lightRequirement: ").append(safe(profile.getLightRequirement())).append("\n");
            context.append("  wateringFrequency: ").append(safe(profile.getWateringFrequency())).append("\n");
            context.append("  humidityRequirement: ").append(safe(profile.getHumidityRequirement())).append("\n");
            context.append("  careLevel: ").append(profile.getCareLevel()).append("\n");
            context.append("  careInstruction: ").append(shortText(profile.getCareInstruction(), 260)).append("\n");
            summary.add("Care " + safe(plant == null ? null : plant.getTitle()) + " - level " + profile.getCareLevel());
        }
    }

    private void appendCouponContext(StringBuilder context, List<String> summary, List<Coupons> coupons) {
        if (coupons.isEmpty()) {
            return;
        }
        context.append("Promotions:\n");
        for (Coupons coupon : coupons) {
            context.append("- id: ").append(safe(coupon.getCouponsId())).append("\n");
            context.append("  code: ").append(safe(coupon.getCode())).append("\n");
            context.append("  type: ").append(coupon.getType()).append("\n");
            context.append("  value: ").append(coupon.getValue()).append("\n");
            context.append("  minOrderAmount: ").append(coupon.getMinOrderAmount()).append("\n");
            context.append("  maxDiscountAmount: ").append(coupon.getMaxDiscountAmount()).append("\n");
            context.append("  maxUses: ").append(coupon.getMaxUses()).append("\n");
            context.append("  usedCount: ").append(coupon.getUsedCount()).append("\n");
            context.append("  perUserLimit: ").append(coupon.getPerUserLimit()).append("\n");
            context.append("  startsAt: ").append(coupon.getStartsAt()).append("\n");
            context.append("  expiresAt: ").append(coupon.getExpiresAt()).append("\n");
            summary.add("Promotion " + safe(coupon.getCode()));
        }
    }

    private void appendOrderContext(StringBuilder context, List<String> summary, List<Orders> orders) {
        if (orders.isEmpty()) {
            return;
        }
        context.append("CurrentUserOrders:\n");
        for (Orders order : orders) {
            context.append("- orderId: ").append(safe(order.getOrderId())).append("\n");
            context.append("  status: ").append(order.getStatus()).append(" (").append(orderStatus(order.getStatus())).append(")\n");
            context.append("  paymentStatus: ").append(order.getPaymentStatus()).append(" (").append(paymentStatus(order.getPaymentStatus())).append(")\n");
            context.append("  subtotal: ").append(order.getSubtotal()).append("\n");
            context.append("  discountAmount: ").append(order.getDiscountAmount()).append("\n");
            context.append("  shippingFee: ").append(order.getShippingFee()).append("\n");
            context.append("  totalPrice: ").append(order.getTotalPrice()).append("\n");
            context.append("  createdAt: ").append(order.getCreatedAt()).append("\n");
            context.append("  items:\n");
            List<OrderItems> items = order.getOrderItemsList() == null ? List.of() : order.getOrderItemsList();
            for (OrderItems item : items) {
                ProductVariant variant = item.getProductVariant();
                Plant plant = variant == null ? null : variant.getPlant();
                context.append("    - plant: ").append(safe(plant == null ? null : plant.getTitle())).append("\n");
                context.append("      variant: ").append(safe(variant == null ? null : variant.getName())).append("\n");
                context.append("      quantity: ").append(item.getQuantity()).append("\n");
                context.append("      unitPrice: ").append(item.getUnitPrice()).append("\n");
                context.append("      totalPrice: ").append(item.getTotalPrice()).append("\n");
            }
            summary.add("Order " + shortId(order.getOrderId()) + " - " + orderStatus(order.getStatus()) + " - " + order.getTotalPrice());
        }
    }

    private BigDecimal effectivePrice(ProductVariant variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        if (variant.getSalePrice() != null && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getSalePrice();
        }
        return variant.getPrice() == null ? BigDecimal.ZERO : variant.getPrice();
    }

    private List<String> searchTerms(String normalizedQuestion) {
        if (normalizedQuestion == null || normalizedQuestion.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String term : normalizedQuestion.split("[^a-z0-9]+")) {
            if (term.length() >= 2 && !STOP_WORDS.contains(term)) {
                terms.add(term);
            }
        }
        return new ArrayList<>(terms);
    }

    private List<String> nonGenericPriceTerms(List<String> terms) {
        Set<String> genericPriceTerms = Set.of(
                "gia", "bao", "nhieu", "tien", "re", "dat", "nhat",
                "khuyen", "mai", "giam", "uu", "dai", "ma", "coupon", "voucher", "code"
        );
        return terms.stream()
                .filter(term -> !genericPriceTerms.contains(term))
                .toList();
    }

    private boolean containsAny(String haystack, String... needles) {
        if (haystack == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String haystack, List<String> needles) {
        if (haystack == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOrderIdLikeText(String normalizedQuestion) {
        return normalizedQuestion != null && normalizedQuestion.matches(".*[a-z0-9]{8,}.*");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D');
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.replace("\n", " ").replace("\r", " ").trim();
    }

    private String shortText(String value, int maxLength) {
        String safe = safe(value);
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength - 3) + "...";
    }

    private String shortId(String value) {
        if (value == null) {
            return "N/A";
        }
        return value.substring(0, Math.min(8, value.length()));
    }

    private String orderStatus(Integer status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case 0 -> "pending";
            case 1 -> "confirmed";
            case 2 -> "shipping";
            case 3 -> "delivered";
            case 4 -> "completed";
            case 5 -> "cancelled";
            default -> "unknown";
        };
    }

    private String paymentStatus(Integer status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case 0 -> "unpaid";
            case 1 -> "paid";
            case 2 -> "failed";
            default -> "unknown";
        };
    }
}
