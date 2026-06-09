package org.example.greenybackend.modules.user.dto;

public record AddressRequest(
        String receiverName,
        String receiverPhone,
        String addressDetail,
        String wardName,
        String districtName,
        String provinceName,
        Integer type,
        Boolean isDefault
) {
}
