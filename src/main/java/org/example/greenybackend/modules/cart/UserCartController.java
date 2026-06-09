package org.example.greenybackend.modules.cart;

import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.cart.dto.CartItemQuantityRequest;
import org.example.greenybackend.modules.cart.dto.CartItemRequest;
import org.example.greenybackend.modules.cart.dto.CartResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/cart")
public class UserCartController {

    private final UserCartService cartService;

    public UserCartController(UserCartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse cart(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return cartService.getCart(currentUser);
    }

    @PostMapping
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody CartItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(currentUser, request));
    }

    @PutMapping("/{cartItemId}")
    public CartResponse updateQuantity(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String cartItemId,
            @RequestBody CartItemQuantityRequest request
    ) {
        return cartService.updateQuantity(currentUser, cartItemId, request);
    }

    @DeleteMapping("/{cartItemId}")
    public CartResponse removeItem(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String cartItemId
    ) {
        return cartService.removeItem(currentUser, cartItemId);
    }

    @DeleteMapping
    public MessageResponse clear(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        cartService.clearCart(currentUser);
        return new MessageResponse("Da xoa toan bo gio hang");
    }
}
