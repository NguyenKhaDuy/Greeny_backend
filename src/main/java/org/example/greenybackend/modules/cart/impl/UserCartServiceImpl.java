package org.example.greenybackend.modules.cart.impl;

import org.example.greenybackend.modules.cart.UserCartService;
import org.example.greenybackend.modules.cart.CartItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.CartItem;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.example.greenybackend.domain.entity.ShippingMethods;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.cart.dto.CartItemQuantityRequest;
import org.example.greenybackend.modules.cart.dto.CartItemRequest;
import org.example.greenybackend.modules.cart.dto.CartItemResponse;
import org.example.greenybackend.modules.cart.dto.CartResponse;
import org.example.greenybackend.modules.order.ShippingMethodsRepository;
import org.example.greenybackend.modules.product.ShopCatalogService;
import org.example.greenybackend.modules.variant.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCartServiceImpl implements UserCartService {

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ShippingMethodsRepository shippingMethodsRepository;
    private final ShopCatalogService catalogService;

    public UserCartServiceImpl(
            CartItemRepository cartItemRepository,
            ProductVariantRepository variantRepository,
            ShippingMethodsRepository shippingMethodsRepository,
            ShopCatalogService catalogService
    ) {
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.shippingMethodsRepository = shippingMethodsRepository;
        this.catalogService = catalogService;
    }

    @Transactional(readOnly = true)
    @Override
    public CartResponse getCart(UserEntity user) {
        return toCartResponse(loadCartItems(user));
    }

    @Transactional
    @Override
    public CartResponse addItem(UserEntity user, CartItemRequest request) {
        if (request == null || request.variantId() == null || request.variantId().isBlank()) {
            throw new IllegalArgumentException("Can chon bien the san pham");
        }
        int quantity = normalizeQuantity(request.quantity());
        ProductVariant variant = variantRepository.findByVariantIdAndIsActiveTrue(request.variantId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bien the san pham kha dung"));
        validateStock(variant, quantity);

        CartItem item = cartItemRepository
                .findByUserEntityUserIdAndProductVariantVariantId(user.getUserId(), variant.getVariantId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setIdCartItem(UUID.randomUUID().toString());
                    newItem.setCreateAt(LocalDateTime.now());
                    newItem.setUserEntity(user);
                    newItem.setProductVariant(variant);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int newQuantity = (item.getQuantity() == null ? 0 : item.getQuantity()) + quantity;
        validateStock(variant, newQuantity);
        item.setQuantity(newQuantity);
        item.setUpdateAt(LocalDateTime.now());
        cartItemRepository.save(item);
        return getCart(user);
    }

    @Transactional
    @Override
    public CartResponse updateQuantity(UserEntity user, String cartItemId, CartItemQuantityRequest request) {
        int quantity = normalizeQuantity(request == null ? null : request.quantity());
        CartItem item = findUserCartItem(user, cartItemId);
        validateStock(item.getProductVariant(), quantity);
        item.setQuantity(quantity);
        item.setUpdateAt(LocalDateTime.now());
        return getCart(user);
    }

    @Transactional
    @Override
    public CartResponse removeItem(UserEntity user, String cartItemId) {
        CartItem item = findUserCartItem(user, cartItemId);
        cartItemRepository.delete(item);
        return getCart(user);
    }

    @Transactional
    @Override
    public void clearCart(UserEntity user) {
        cartItemRepository.deleteByUserEntityUserId(user.getUserId());
    }

    @Transactional(readOnly = true)
    @Override
    public List<CartItem> loadCartItems(UserEntity user) {
        return cartItemRepository.findByUserEntityUserIdOrderByCreateAtDesc(user.getUserId()).stream()
                .sorted(Comparator.comparing(CartItem::getCreateAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CartItem findUserCartItem(UserEntity user, String cartItemId) {
        if (cartItemId == null || cartItemId.isBlank()) {
            throw new IllegalArgumentException("Khong tim thay san pham trong gio hang");
        }
        return cartItemRepository.findByIdCartItemAndUserEntityUserId(cartItemId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham trong gio hang"));
    }

    @Override
    public CartResponse toCartResponse(List<CartItem> items) {
        List<CartItemResponse> responses = items.stream().map(this::toItemResponse).toList();
        BigDecimal subtotal = responses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = responses.isEmpty() ? BigDecimal.ZERO : currentShippingFee();
        int totalItems = responses.stream()
                .map(CartItemResponse::quantity)
                .mapToInt(value -> value == null ? 0 : value)
                .sum();
        return new CartResponse(
                responses,
                subtotal,
                BigDecimal.ZERO,
                shippingFee,
                subtotal.add(shippingFee),
                totalItems
        );
    }

    @Override
    public CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        Plant plant = variant == null ? null : variant.getPlant();
        BigDecimal unitPrice = catalogService.effectivePrice(variant);
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        int stock = variant == null || variant.getQuantity() == null ? 0 : Math.max(variant.getQuantity(), 0);
        return new CartItemResponse(
                item.getIdCartItem(),
                variant == null ? null : variant.getVariantId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                variant == null ? null : variant.getName(),
                variant == null ? null : variant.getSku(),
                variant == null ? null : catalogService.firstImage(variant),
                unitPrice,
                variant == null ? BigDecimal.ZERO : variant.getPrice(),
                quantity,
                stock,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                variant != null && Boolean.TRUE.equals(variant.getIsActive()) && stock >= quantity && quantity > 0
        );
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("So luong phai lon hon 0");
        }
        return quantity;
    }

    private void validateStock(ProductVariant variant, int requestedQuantity) {
        if (variant == null || !Boolean.TRUE.equals(variant.getIsActive())) {
            throw new IllegalArgumentException("San pham khong kha dung");
        }
        int stock = variant.getQuantity() == null ? 0 : variant.getQuantity();
        if (stock <= 0) {
            throw new IllegalArgumentException("San pham da het hang");
        }
        if (requestedQuantity > stock) {
            throw new IllegalArgumentException("So luong vuot qua ton kho hien tai");
        }
    }

    private BigDecimal currentShippingFee() {
        return shippingMethodsRepository.findFirstByIsActiveTrueOrderBySortOrderAsc()
                .map(ShippingMethods::getBaseFee)
                .filter(value -> value.compareTo(BigDecimal.ZERO) >= 0)
                .orElse(DEFAULT_SHIPPING_FEE);
    }
}
