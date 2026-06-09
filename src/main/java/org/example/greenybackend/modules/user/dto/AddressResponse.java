package org.example.greenybackend.modules.user.dto;

import java.time.LocalDateTime;

public record AddressResponse(
        String addressId,
        String receiverName,
        String receiverPhone,
        String addressDetail,
        String wardName,
        String districtName,
        String provinceName,
        String fullAddress,
        Integer type,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
