package org.example.greenybackend.modules.order.dto;

import java.util.List;
import org.example.greenybackend.modules.user.dto.AddressRequest;

public record CheckoutRequest(
        List<String> cartItemIds,
        String addressId,
        AddressRequest address,
        String couponCode,
        String paymentMethod,
        String notes
) {
}
