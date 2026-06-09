package org.example.greenybackend.modules.order.impl;

import org.example.greenybackend.modules.order.UserCheckoutService;
import org.example.greenybackend.modules.order.OrderItemsRepository;
import org.example.greenybackend.modules.order.OrdersRepository;
import org.example.greenybackend.modules.order.ShipmentsRepository;
import org.example.greenybackend.modules.order.ShippingMethodsRepository;
import org.example.greenybackend.modules.order.UserOrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Address;
import org.example.greenybackend.domain.entity.CartItem;
import org.example.greenybackend.domain.entity.Coupons;
import org.example.greenybackend.domain.entity.CouponUsages;
import org.example.greenybackend.domain.entity.OrderItems;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.Payments;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.domain.entity.Shipments;
import org.example.greenybackend.domain.entity.ShippingMethods;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.cart.CartItemRepository;
import org.example.greenybackend.modules.cart.UserCartService;
import org.example.greenybackend.modules.notification.UserNotificationService;
import org.example.greenybackend.modules.order.dto.CheckoutRequest;
import org.example.greenybackend.modules.order.dto.UserOrderResponse;
import org.example.greenybackend.modules.payment.dto.PaymentMockRequest;
import org.example.greenybackend.modules.payment.PaymentsRepository;
import org.example.greenybackend.modules.product.ShopCatalogService;
import org.example.greenybackend.modules.promotion.CouponsRepository;
import org.example.greenybackend.modules.promotion.CouponUsagesRepository;
import org.example.greenybackend.modules.promotion.dto.CouponApplyRequest;
import org.example.greenybackend.modules.promotion.dto.CouponPreviewResponse;
import org.example.greenybackend.modules.user.UserAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCheckoutServiceImpl implements UserCheckoutService {

    private static final String NO_COUPON_CODE = "NO_COUPON";
    private static final String PAYMENT_METHOD_COD = "COD";
    private static final String PAYMENT_METHOD_ONLINE = "ONLINE";
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    private final UserCartService cartService;
    private final UserAccountService accountService;
    private final UserOrderService orderService;
    private final UserNotificationService notificationService;
    private final CartItemRepository cartItemRepository;
    private final CouponsRepository couponsRepository;
    private final CouponUsagesRepository couponUsagesRepository;
    private final PaymentsRepository paymentsRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final ShippingMethodsRepository shippingMethodsRepository;
    private final ShipmentsRepository shipmentsRepository;
    private final ShopCatalogService catalogService;

    public UserCheckoutServiceImpl(
            UserCartService cartService,
            UserAccountService accountService,
            UserOrderService orderService,
            UserNotificationService notificationService,
            CartItemRepository cartItemRepository,
            CouponsRepository couponsRepository,
            CouponUsagesRepository couponUsagesRepository,
            PaymentsRepository paymentsRepository,
            OrdersRepository ordersRepository,
            OrderItemsRepository orderItemsRepository,
            ShippingMethodsRepository shippingMethodsRepository,
            ShipmentsRepository shipmentsRepository,
            ShopCatalogService catalogService
    ) {
        this.cartService = cartService;
        this.accountService = accountService;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.cartItemRepository = cartItemRepository;
        this.couponsRepository = couponsRepository;
        this.couponUsagesRepository = couponUsagesRepository;
        this.paymentsRepository = paymentsRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.shippingMethodsRepository = shippingMethodsRepository;
        this.shipmentsRepository = shipmentsRepository;
        this.catalogService = catalogService;
    }

    @Transactional(readOnly = true)
    @Override
    public CouponPreviewResponse previewCoupon(UserEntity user, CouponApplyRequest request) {
        BigDecimal subtotal = request == null || request.subtotal() == null ? BigDecimal.ZERO : request.subtotal();
        CouponCalculation calculation = calculateCoupon(user, request == null ? null : request.couponCode(), subtotal);
        Coupons coupon = calculation.coupon();
        return new CouponPreviewResponse(
                coupon == null ? null : coupon.getCouponsId(),
                coupon == null ? null : coupon.getCode(),
                coupon == null ? null : coupon.getType(),
                coupon == null ? null : coupon.getValue(),
                calculation.discountAmount(),
                calculation.message(),
                true
        );
    }

    @Transactional
    @Override
    public UserOrderResponse checkout(UserEntity user, CheckoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu dat hang khong hop le");
        }

        List<CartItem> selectedItems = loadSelectedCartItems(user, request.cartItemIds());
        if (selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Gio hang dang rong");
        }
        selectedItems.forEach(this::validateCartItemForCheckout);

        Address address = resolveAddress(user, request);
        ShippingMethods shippingMethod = ensureShippingMethod();
        BigDecimal shippingFee = shippingMethod.getBaseFee() == null ? DEFAULT_SHIPPING_FEE : shippingMethod.getBaseFee();
        BigDecimal subtotal = subtotal(selectedItems);
        CouponCalculation couponCalculation = calculateCoupon(user, request.couponCode(), subtotal);
        Coupons appliedCoupon = couponCalculation.coupon() == null ? ensureNoCoupon() : couponCalculation.coupon();
        BigDecimal totalPrice = subtotal.subtract(couponCalculation.discountAmount()).add(shippingFee);
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        Payments payment = createPayment(request.paymentMethod(), totalPrice, now);
        Shipments shipment = createShipment(shippingFee, now);

        Orders order = new Orders();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserEntity(user);
        order.setAddress(address);
        order.setShippingMethods(shippingMethod);
        order.setShipments(shipment);
        order.setPayments(payment);
        order.setCoupons(appliedCoupon);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(couponCalculation.discountAmount());
        order.setShippingFee(shippingFee);
        order.setTotalPrice(totalPrice);
        order.setStatus(UserOrderService.ORDER_PENDING);
        order.setPaymentStatus(payment.getStatus());
        order.setNotes(trimToNull(request.notes()));
        order.setEstimatedDelivery("2-4 ngay lam viec");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        ordersRepository.save(order);

        List<OrderItems> orderItems = selectedItems.stream()
                .map(cartItem -> createOrderItem(order, cartItem, now))
                .toList();
        orderItemsRepository.saveAll(orderItems);
        order.getOrderItemsList().addAll(orderItems);

        selectedItems.forEach(this::decreaseStock);
        if (couponCalculation.coupon() != null) {
            applyCouponUsage(user, couponCalculation.coupon(), couponCalculation.discountAmount(), now);
        }
        cartItemRepository.deleteAll(selectedItems);
        orderService.addStatusHistory(order, null, UserOrderService.ORDER_PENDING, "User tao don hang");

        notificationService.sendToUser(
                user,
                1,
                "Dat hang thanh cong",
                "Don " + shortOrderCode(order.getOrderId()) + " da duoc tao. Tong thanh toan: " + totalPrice + ".",
                "{\"orderId\":\"" + order.getOrderId() + "\"}"
        );
        return orderService.toResponse(order);
    }

    @Transactional
    @Override
    public UserOrderResponse completeMockPayment(UserEntity user, String orderId, PaymentMockRequest request) {
        Orders order = orderService.findUserOrder(user, orderId);
        Payments payment = order.getPayments();
        if (payment == null) {
            throw new IllegalArgumentException("Don hang chua co thong tin thanh toan");
        }
        if (order.getStatus() != null && order.getStatus() == UserOrderService.ORDER_CANCELED) {
            throw new IllegalArgumentException("Don hang da huy khong the thanh toan");
        }
        if (payment.getMethod() == null || !payment.getMethod().startsWith(PAYMENT_METHOD_ONLINE)) {
            throw new IllegalArgumentException("Don hang khong su dung thanh toan online");
        }

        String result = request == null || request.result() == null ? "SUCCESS" : request.result().trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if ("SUCCESS".equals(result) || "PAID".equals(result)) {
            payment.setStatus(UserOrderService.PAYMENT_PAID);
            payment.setPaidAt(now);
            payment.setGatewayResponse("Mock online payment success");
            notificationService.sendToUser(
                    user,
                    2,
                    "Thanh toan thanh cong",
                    "Don " + shortOrderCode(order.getOrderId()) + " da thanh toan thanh cong.",
                    "{\"orderId\":\"" + order.getOrderId() + "\"}"
            );
        } else if ("FAILED".equals(result) || "FAIL".equals(result)) {
            payment.setStatus(UserOrderService.PAYMENT_FAILED);
            payment.setGatewayResponse("Mock online payment failed");
            notificationService.sendToUser(
                    user,
                    2,
                    "Thanh toan that bai",
                    "Thanh toan cho don " + shortOrderCode(order.getOrderId()) + " chua thanh cong.",
                    "{\"orderId\":\"" + order.getOrderId() + "\"}"
            );
        } else {
            throw new IllegalArgumentException("Ket qua thanh toan mock khong hop le");
        }
        if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            payment.setTransactionId("MOCK-" + UUID.randomUUID());
        }
        payment.setUpdatedAt(now);
        order.setPaymentStatus(payment.getStatus());
        order.setUpdatedAt(now);
        return orderService.toResponse(order);
    }

    private List<CartItem> loadSelectedCartItems(UserEntity user, List<String> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return cartService.loadCartItems(user);
        }
        Set<String> uniqueIds = new LinkedHashSet<>(cartItemIds);
        return uniqueIds.stream()
                .map(cartItemId -> cartService.findUserCartItem(user, cartItemId))
                .toList();
    }

    private Address resolveAddress(UserEntity user, CheckoutRequest request) {
        if (request.addressId() != null && !request.addressId().isBlank()) {
            return accountService.findUserAddress(user, request.addressId());
        }
        if (request.address() != null) {
            String addressId = accountService.createAddress(user, request.address()).addressId();
            return accountService.findUserAddress(user, addressId);
        }
        throw new IllegalArgumentException("Can chon hoac them dia chi giao hang");
    }

    private Payments createPayment(String requestedMethod, BigDecimal amount, LocalDateTime now) {
        String method = normalizePaymentMethod(requestedMethod);
        Payments payment = new Payments();
        payment.setPaymentsId(UUID.randomUUID().toString());
        payment.setAmount(amount);
        payment.setMethod(PAYMENT_METHOD_ONLINE.equals(method) ? "ONLINE_MOCK" : PAYMENT_METHOD_COD);
        payment.setStatus(UserOrderService.PAYMENT_PENDING);
        payment.setGatewayResponse(PAYMENT_METHOD_ONLINE.equals(method) ? "Waiting for mock online payment" : "COD payment pending");
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return paymentsRepository.save(payment);
    }

    private Shipments createShipment(BigDecimal shippingFee, LocalDateTime now) {
        Shipments shipment = new Shipments();
        shipment.setShipmentsId(UUID.randomUUID().toString());
        shipment.setCarrierName("Greeny Delivery");
        shipment.setTrackingCode("GRN-" + now.toLocalDate().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        shipment.setStatus(0);
        shipment.setShippingFee(shippingFee);
        shipment.setNote("Cho xu ly");
        shipment.setCreatedAt(now);
        shipment.setUpdatedAt(now);
        return shipmentsRepository.save(shipment);
    }

    private ShippingMethods ensureShippingMethod() {
        return shippingMethodsRepository.findFirstByIsActiveTrueOrderBySortOrderAsc()
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    ShippingMethods method = new ShippingMethods();
                    method.setShipId(UUID.randomUUID().toString());
                    method.setName("Giao hang tieu chuan");
                    method.setCode("STANDARD");
                    method.setBaseFee(DEFAULT_SHIPPING_FEE);
                    method.setDescription("Giao hang noi dia tieu chuan");
                    method.setIsActive(true);
                    method.setSortOrder(1);
                    method.setCreatedAt(now);
                    method.setUpdatedAt(now);
                    return shippingMethodsRepository.save(method);
                });
    }

    private Coupons ensureNoCoupon() {
        return couponsRepository.findByCodeIgnoreCase(NO_COUPON_CODE)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Coupons coupon = new Coupons();
                    coupon.setCouponsId(UUID.randomUUID().toString());
                    coupon.setCode(NO_COUPON_CODE);
                    coupon.setType(2);
                    coupon.setValue(BigDecimal.ZERO);
                    coupon.setMinOrderAmount(BigDecimal.ZERO);
                    coupon.setUsedCount(0);
                    coupon.setIsActive(true);
                    coupon.setCreatedAt(now);
                    coupon.setUpdatedAt(now);
                    return couponsRepository.save(coupon);
                });
    }

    private CouponCalculation calculateCoupon(UserEntity user, String couponCode, BigDecimal subtotal) {
        String normalizedCode = normalizeCouponCode(couponCode);
        if (normalizedCode == null) {
            return new CouponCalculation(null, BigDecimal.ZERO, "Khong ap dung ma giam gia");
        }
        Coupons coupon = couponsRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Ma giam gia khong ton tai"));
        validateCoupon(user, coupon, subtotal);

        BigDecimal discount = switch (coupon.getType() == null ? 2 : coupon.getType()) {
            case 1 -> subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100));
            case 2 -> coupon.getValue();
            default -> throw new IllegalArgumentException("Loai ma giam gia khong hop le");
        };
        if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        discount = discount.min(subtotal).max(BigDecimal.ZERO);
        return new CouponCalculation(coupon, discount, "Ma giam gia hop le");
    }

    private void validateCoupon(UserEntity user, Coupons coupon, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new IllegalArgumentException("Ma giam gia da bi vo hieu hoa");
        }
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException("Ma giam gia chua den thoi gian su dung");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Ma giam gia da het han");
        }
        int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        if (coupon.getMaxUses() != null && coupon.getMaxUses() > 0 && usedCount >= coupon.getMaxUses()) {
            throw new IllegalArgumentException("Ma giam gia da het luot su dung");
        }
        if (coupon.getPerUserLimit() != null && coupon.getPerUserLimit() > 0) {
            long userUsedCount = couponUsagesRepository.countByCouponsCouponsIdAndUserEntityUserId(coupon.getCouponsId(), user.getUserId());
            if (userUsedCount >= coupon.getPerUserLimit()) {
                throw new IllegalArgumentException("Ban da dung ma giam gia nay toi da so lan cho phep");
            }
        }
        BigDecimal minOrderAmount = coupon.getMinOrderAmount() == null ? BigDecimal.ZERO : coupon.getMinOrderAmount();
        if (subtotal.compareTo(minOrderAmount) < 0) {
            throw new IllegalArgumentException("Don hang chua dat gia tri toi thieu de dung ma");
        }
        if (coupon.getValue() == null || coupon.getValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Gia tri ma giam gia khong hop le");
        }
    }

    private OrderItems createOrderItem(Orders order, CartItem cartItem, LocalDateTime now) {
        ProductVariant variant = cartItem.getProductVariant();
        BigDecimal unitPrice = catalogService.effectivePrice(variant);
        int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();

        OrderItems item = new OrderItems();
        item.setIdOrderItem(UUID.randomUUID().toString());
        item.setOrders(order);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    private void validateCartItemForCheckout(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        if (variant == null || !Boolean.TRUE.equals(variant.getIsActive())) {
            throw new IllegalArgumentException("Gio hang co san pham khong kha dung");
        }
        int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Gio hang co san pham so luong khong hop le");
        }
        int stock = variant.getQuantity() == null ? 0 : variant.getQuantity();
        if (stock <= 0) {
            throw new IllegalArgumentException("San pham " + variant.getName() + " da het hang");
        }
        if (quantity > stock) {
            throw new IllegalArgumentException("San pham " + variant.getName() + " vuot qua ton kho hien tai");
        }
    }

    private void decreaseStock(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        int stock = variant.getQuantity() == null ? 0 : variant.getQuantity();
        variant.setQuantity(stock - cartItem.getQuantity());
        variant.setUpdatedAt(LocalDateTime.now());
    }

    private void applyCouponUsage(UserEntity user, Coupons coupon, BigDecimal discountAmount, LocalDateTime now) {
        coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
        coupon.setUpdatedAt(now);

        CouponUsages usage = new CouponUsages();
        usage.setIdCouponUsages(UUID.randomUUID().toString());
        usage.setCoupons(coupon);
        usage.setUserEntity(user);
        usage.setDiscountAmount(discountAmount);
        usage.setUsedAt(now);
        couponUsagesRepository.save(usage);
    }

    private BigDecimal subtotal(List<CartItem> selectedItems) {
        return selectedItems.stream()
                .map(item -> catalogService.effectivePrice(item.getProductVariant())
                        .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PAYMENT_METHOD_COD;
        }
        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (PAYMENT_METHOD_COD.equals(normalized) || PAYMENT_METHOD_ONLINE.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le");
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase(Locale.ROOT);
    }

    private String shortOrderCode(String orderId) {
        if (orderId == null || orderId.length() <= 8) {
            return orderId == null ? "" : orderId;
        }
        return orderId.substring(0, 8);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record CouponCalculation(Coupons coupon, BigDecimal discountAmount, String message) {
    }
}
