package org.example.greenybackend.modules.order.impl;

import org.example.greenybackend.modules.order.UserOrderService;
import org.example.greenybackend.modules.order.OrderStatusHistoryRepository;
import org.example.greenybackend.modules.order.OrdersRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Address;
import org.example.greenybackend.domain.entity.Coupons;
import org.example.greenybackend.domain.entity.OrderItems;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.OrderStatusHistory;
import org.example.greenybackend.domain.entity.Payments;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.domain.entity.Shipments;
import org.example.greenybackend.domain.entity.ShippingMethods;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.notification.UserNotificationService;
import org.example.greenybackend.modules.order.dto.OrderHistoryResponse;
import org.example.greenybackend.modules.order.dto.OrderItemResponse;
import org.example.greenybackend.modules.order.dto.UserOrderResponse;
import org.example.greenybackend.modules.payment.dto.PaymentResponse;
import org.example.greenybackend.modules.product.ShopCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOrderServiceImpl implements UserOrderService {

    public static final int ORDER_PENDING = 0;
    public static final int ORDER_CONFIRMED = 1;
    public static final int ORDER_PREPARING = 2;
    public static final int ORDER_SHIPPING = 3;
    public static final int ORDER_DELIVERED = 4;
    public static final int ORDER_CANCELED = 5;
    public static final int ORDER_RETURNED = 6;

    public static final int PAYMENT_PENDING = 0;
    public static final int PAYMENT_PAID = 1;
    public static final int PAYMENT_FAILED = 2;
    public static final int PAYMENT_REFUNDED = 3;
    public static final int PAYMENT_CANCELED = 4;

    private static final String NO_COUPON_CODE = "NO_COUPON";

    private final OrdersRepository ordersRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final UserNotificationService notificationService;
    private final ShopCatalogService catalogService;

    public UserOrderServiceImpl(
            OrdersRepository ordersRepository,
            OrderStatusHistoryRepository historyRepository,
            UserNotificationService notificationService,
            ShopCatalogService catalogService
    ) {
        this.ordersRepository = ordersRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
        this.catalogService = catalogService;
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserOrderResponse> getOrders(UserEntity user) {
        return ordersRepository.findByUserEntityUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public UserOrderResponse getOrder(UserEntity user, String orderId) {
        return toResponse(findUserOrder(user, orderId));
    }

    @Transactional
    @Override
    public UserOrderResponse cancelOrder(UserEntity user, String orderId) {
        Orders order = findUserOrder(user, orderId);
        if (!canCancel(order)) {
            throw new IllegalArgumentException("Don hang hien tai khong the huy");
        }
        Integer oldStatus = order.getStatus();
        order.setStatus(ORDER_CANCELED);
        order.setUpdatedAt(LocalDateTime.now());

        Payments payment = order.getPayments();
        if (payment != null) {
            if (payment.getStatus() != null && payment.getStatus() == PAYMENT_PAID) {
                payment.setStatus(PAYMENT_REFUNDED);
                payment.setGatewayResponse("Mock refund after user cancellation");
            } else {
                payment.setStatus(PAYMENT_CANCELED);
                payment.setGatewayResponse("Canceled by user before payment completion");
            }
            payment.setUpdatedAt(LocalDateTime.now());
            order.setPaymentStatus(payment.getStatus());
        }

        restoreStock(order);
        addStatusHistory(order, oldStatus, ORDER_CANCELED, "User huy don hang");
        notificationService.sendToUser(
                user,
                2,
                "Don hang da huy",
                "Don " + shortOrderCode(order.getOrderId()) + " da duoc huy thanh cong.",
                "{\"orderId\":\"" + order.getOrderId() + "\"}"
        );
        return toResponse(order);
    }

    @Override
    public Orders findUserOrder(UserEntity user, String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Khong tim thay don hang");
        }
        return ordersRepository.findByOrderIdAndUserEntityUserId(orderId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang"));
    }

    @Override
    public OrderStatusHistory addStatusHistory(Orders order, Integer oldStatus, Integer newStatus, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderStatusHistoryId(UUID.randomUUID().toString());
        history.setOrders(order);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNote(trimToNull(note));
        history.setCreatedAt(LocalDateTime.now());
        historyRepository.save(history);
        order.getOrderStatusHistories().add(history);
        return history;
    }

    @Override
    public UserOrderResponse toResponse(Orders order) {
        Address address = order.getAddress();
        ShippingMethods shippingMethod = order.getShippingMethods();
        Shipments shipment = order.getShipments();
        Coupons coupon = order.getCoupons();
        String couponCode = coupon == null || NO_COUPON_CODE.equalsIgnoreCase(coupon.getCode()) ? null : coupon.getCode();

        List<OrderItemResponse> items = order.getOrderItemsList().stream()
                .map(this::toItemResponse)
                .toList();
        List<OrderHistoryResponse> histories = order.getOrderStatusHistories().stream()
                .sorted(Comparator.comparing(OrderStatusHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toHistoryResponse)
                .toList();

        return new UserOrderResponse(
                order.getOrderId(),
                address == null ? null : address.getReceiverName(),
                address == null ? null : address.getReceiverPhone(),
                formatAddress(address),
                shippingMethod == null ? null : shippingMethod.getName(),
                shipment == null ? null : shipment.getCarrierName(),
                shipment == null ? null : shipment.getTrackingCode(),
                shipment == null ? null : shipment.getStatus(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getStatus(),
                orderStatusLabel(order.getStatus()),
                order.getPaymentStatus(),
                paymentStatusLabel(order.getPaymentStatus()),
                order.getNotes(),
                order.getEstimatedDelivery(),
                couponCode,
                toPaymentResponse(order.getPayments()),
                items,
                histories,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                canCancel(order),
                order.getStatus() != null && order.getStatus() == ORDER_DELIVERED
        );
    }

    @Override
    public String orderStatusLabel(Integer status) {
        if (status == null) {
            return "Chua ro";
        }
        return switch (status) {
            case ORDER_PENDING -> "Cho xac nhan";
            case ORDER_CONFIRMED -> "Da xac nhan";
            case ORDER_PREPARING -> "Dang chuan bi";
            case ORDER_SHIPPING -> "Dang giao";
            case ORDER_DELIVERED -> "Da giao";
            case ORDER_CANCELED -> "Da huy";
            case ORDER_RETURNED -> "Hoan tra";
            default -> "Trang thai " + status;
        };
    }

    @Override
    public String paymentStatusLabel(Integer status) {
        if (status == null) {
            return "Chua ro";
        }
        return switch (status) {
            case PAYMENT_PENDING -> "Cho thanh toan";
            case PAYMENT_PAID -> "Da thanh toan";
            case PAYMENT_FAILED -> "Thanh toan loi";
            case PAYMENT_REFUNDED -> "Da hoan tien";
            case PAYMENT_CANCELED -> "Da huy";
            default -> "Trang thai " + status;
        };
    }

    private boolean canCancel(Orders order) {
        return order.getStatus() != null
                && (order.getStatus() == ORDER_PENDING || order.getStatus() == ORDER_CONFIRMED);
    }

    private void restoreStock(Orders order) {
        for (OrderItems item : order.getOrderItemsList()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null || item.getQuantity() == null) {
                continue;
            }
            int stock = variant.getQuantity() == null ? 0 : variant.getQuantity();
            variant.setQuantity(stock + item.getQuantity());
            variant.setUpdatedAt(LocalDateTime.now());
        }
    }

    private OrderItemResponse toItemResponse(OrderItems item) {
        ProductVariant variant = item.getProductVariant();
        Plant plant = variant == null ? null : variant.getPlant();
        return new OrderItemResponse(
                item.getIdOrderItem(),
                variant == null ? null : variant.getVariantId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                variant == null ? null : variant.getName(),
                variant == null ? null : variant.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                variant == null ? null : catalogService.firstImage(variant)
        );
    }

    private OrderHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        return new OrderHistoryResponse(
                history.getOrderStatusHistoryId(),
                history.getOldStatus(),
                history.getNewStatus(),
                orderStatusLabel(history.getOldStatus()),
                orderStatusLabel(history.getNewStatus()),
                history.getNote(),
                history.getCreatedAt()
        );
    }

    private PaymentResponse toPaymentResponse(Payments payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getPaymentsId(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                paymentStatusLabel(payment.getStatus()),
                payment.getGatewayResponse(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return null;
        }
        return Stream.of(address.getAddressDetail(), address.getWardName(), address.getDistrictName(), address.getProvinceName())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
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
}
