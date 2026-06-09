package org.example.greenybackend.modules.user;

import java.util.List;
import org.example.greenybackend.domain.entity.Address;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.dto.AddressRequest;
import org.example.greenybackend.modules.user.dto.AddressResponse;
import org.example.greenybackend.modules.user.dto.PasswordChangeRequest;
import org.example.greenybackend.modules.user.dto.UserProfileResponse;
import org.example.greenybackend.modules.user.dto.UserProfileUpdateRequest;

public interface UserAccountService {

    UserProfileResponse getProfile(UserEntity user);

    UserProfileResponse updateProfile(UserEntity user, UserProfileUpdateRequest request);

    void changePassword(UserEntity user, PasswordChangeRequest request);

    List<AddressResponse> getAddresses(UserEntity user);

    AddressResponse createAddress(UserEntity user, AddressRequest request);

    AddressResponse updateAddress(UserEntity user, String addressId, AddressRequest request);

    AddressResponse setDefaultAddress(UserEntity user, String addressId);

    void deleteAddress(UserEntity user, String addressId);

    Address findUserAddress(UserEntity user, String addressId);

    AddressResponse toAddressResponse(Address address);

}
