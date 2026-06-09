package org.example.greenybackend.modules.order.impl;

import org.example.greenybackend.modules.order.AdminOrderService;
import org.example.greenybackend.modules.order.OrderStatusHistoryRepository;
import org.example.greenybackend.modules.order.OrdersRepository;
import java.math.BigDecimal;
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
import org.example.greenybackend.modules.order.dto.AdminOrderHistoryResponse;
import org.example.greenybackend.modules.order.dto.AdminOrderItemResponse;
import org.example.greenybackend.modules.order.dto.AdminOrderResponse;
import org.example.greenybackend.modules.order.dto.OrderStatusUpdateRequest;
import org.example.greenybackend.modules.payment.dto.AdminPaymentResponse;
import org.example.greenybackend.modules.payment.dto.PaymentUpdateRequest;
import org.example.greenybackend.modules.payment.PaymentsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final int PAYMENT_STATUS_PAID = 1;

    private final OrdersRepository ordersRepository;
    private final PaymentsRepository paymentsRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public AdminOrderServiceImpl(
            OrdersRepository ordersRepository,
            PaymentsRepository paymentsRepository,
            OrderStatusHistoryRepository historyRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.paymentsRepository = paymentsRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdminOrderResponse> getAllOrders(Integer status, Integer paymentStatus) {
        return ordersRepository.findAll().stream()
                .filter(order -> status == null || status.equals(order.getStatus()))
                .filter(order -> paymentStatus == null || paymentStatus.equals(order.getPaymentStatus()))
                .sorted(Comparator.comparing(Orders::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public AdminOrderResponse getOrder(String orderId) {
        return toResponse(findOrder(orderId));
    }

    @Transactional
    @Override
    public AdminOrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request) {
        Orders order = findOrder(orderId);
        Integer oldStatus = order.getStatus();
        boolean changed = request.status() != null && !request.status().equals(oldStatus);

        if (request.status() != null) {
            order.setStatus(request.status());
        }
        if (request.paymentStatus() != null) {
            order.setPaymentStatus(request.paymentStatus());
            Payments payment = order.getPayments();
            if (payment != null) {
                payment.setStatus(request.paymentStatus());
                if (request.paymentStatus() == PAYMENT_STATUS_PAID && payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
                payment.setUpdatedAt(LocalDateTime.now());
            }
        }
        order.setEstimatedDelivery(trimToNull(request.estimatedDelivery()));
        order.setUpdatedAt(LocalDateTime.now());

        if (changed) {
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrderStatusHistoryId(UUID.randomUUID().toString());
            history.setOrders(order);
            history.setOldStatus(oldStatus);
            history.setNewStatus(request.status());
            history.setNote(trimToNull(request.note()));
            history.setCreatedAt(LocalDateTime.now());
            historyRepository.save(history);
            order.getOrderStatusHistories().add(history);
        }

        return toResponse(order);
    }

    @Transactional
    @Override
    public AdminOrderResponse updatePayment(String orderId, PaymentUpdateRequest request) {
        Orders order = findOrder(orderId);
        Payments payment = order.getPayments();
        if (payment == null) {
            throw new IllegalArgumentException("Đơn hàng chưa có bản ghi thanh toán");
        }

        payment.setTransactionId(trimToNull(request.transactionId()));
        payment.setAmount(defaultMoney(request.amount(), payment.getAmount()));
        payment.setMethod(trimToNull(request.method()));
        if (request.status() != null) {
            payment.setStatus(request.status());
            order.setPaymentStatus(request.status());
            if (request.status() == PAYMENT_STATUS_PAID && request.paidAt() == null && payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
        }
        payment.setGatewayResponse(trimToNull(request.gatewayResponse()));
        if (request.paidAt() != null) {
            payment.setPaidAt(request.paidAt());
        }
        payment.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        paymentsRepository.save(payment);
        return toResponse(order);
    }

    private Orders findOrder(String orderId) {
        return ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    private AdminOrderResponse toResponse(Orders order) {
        UserEntity user = order.getUserEntity();
        Address address = order.getAddress();
        ShippingMethods shippingMethod = order.getShippingMethods();
        Shipments shipment = order.getShipments();
        Coupons coupon = order.getCoupons();

        return new AdminOrderResponse(
                order.getOrderId(),
                user == null ? null : user.getUserId(),
                user == null ? null : user.getTitle(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
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
                coupon == null ? null : coupon.getCode(),
                toPaymentResponse(order.getPayments()),
                order.getOrderItemsList().stream().map(this::toItemResponse).toList(),
                order.getOrderStatusHistories().stream()
                        .sorted(Comparator.comparing(OrderStatusHistory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(this::toHistoryResponse)
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private AdminOrderItemResponse toItemResponse(OrderItems item) {
        ProductVariant variant = item.getProductVariant();
        Plant plant = variant == null ? null : variant.getPlant();
        return new AdminOrderItemResponse(
                item.getIdOrderItem(),
                variant == null ? null : variant.getVariantId(),
                plant == null ? null : plant.getTitle(),
                variant == null ? null : variant.getName(),
                variant == null ? null : variant.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }

    private AdminOrderHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        return new AdminOrderHistoryResponse(
                history.getOrderStatusHistoryId(),
                history.getOldStatus(),
                orderStatusLabel(history.getOldStatus()),
                history.getNewStatus(),
                orderStatusLabel(history.getNewStatus()),
                history.getNote(),
                history.getCreatedAt()
        );
    }

    private AdminPaymentResponse toPaymentResponse(Payments payment) {
        if (payment == null) {
            return null;
        }
        return new AdminPaymentResponse(
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
        return joinAddress(address.getAddressDetail(), address.getWardName(), address.getDistrictName(), address.getProvinceName());
    }

    private String joinAddress(String detail, String ward, String district, String province) {
        return Stream.of(detail, ward, district, province)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    @Override
    public String orderStatusLabel(Integer status) {
        if (status == null) {
            return "Chưa rõ";
        }
        return switch (status) {
            case 0 -> "Chờ xác nhận";
            case 1 -> "Đã xác nhận";
            case 2 -> "Đang chuẩn bị";
            case 3 -> "Đang giao";
            case 4 -> "Đã giao";
            case 5 -> "Đã hủy";
            case 6 -> "Hoàn trả";
            default -> "Trạng thái " + status;
        };
    }

    @Override
    public String paymentStatusLabel(Integer status) {
        if (status == null) {
            return "Chưa rõ";
        }
        return switch (status) {
            case 0 -> "Chờ thanh toán";
            case 1 -> "Đã thanh toán";
            case 2 -> "Thanh toán thất bại";
            case 3 -> "Hoàn tiền";
            case 4 -> "Đã hủy";
            default -> "Trạng thái " + status;
        };
    }

    private BigDecimal defaultMoney(BigDecimal requestAmount, BigDecimal currentAmount) {
        return requestAmount == null ? currentAmount : requestAmount;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
