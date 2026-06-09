package org.example.greenybackend.modules.cart;

import java.util.List;
import org.example.greenybackend.domain.entity.CartItem;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.cart.dto.CartItemQuantityRequest;
import org.example.greenybackend.modules.cart.dto.CartItemRequest;
import org.example.greenybackend.modules.cart.dto.CartItemResponse;
import org.example.greenybackend.modules.cart.dto.CartResponse;

public interface UserCartService {

    CartResponse getCart(UserEntity user);

    CartResponse addItem(UserEntity user, CartItemRequest request);

    CartResponse updateQuantity(UserEntity user, String cartItemId, CartItemQuantityRequest request);

    CartResponse removeItem(UserEntity user, String cartItemId);

    void clearCart(UserEntity user);

    List<CartItem> loadCartItems(UserEntity user);

    CartItem findUserCartItem(UserEntity user, String cartItemId);

    CartResponse toCartResponse(List<CartItem> items);

    CartItemResponse toItemResponse(CartItem item);

}
