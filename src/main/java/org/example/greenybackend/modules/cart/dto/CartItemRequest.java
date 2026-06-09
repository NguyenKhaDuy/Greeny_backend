package org.example.greenybackend.modules.cart.dto;

public record CartItemRequest(
        String variantId,
        Integer quantity
) {
}
