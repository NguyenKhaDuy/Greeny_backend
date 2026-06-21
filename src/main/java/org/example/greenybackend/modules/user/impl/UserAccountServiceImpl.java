package org.example.greenybackend.modules.user.impl;

import org.example.greenybackend.common.util.ImageStorageService;
import org.example.greenybackend.common.util.ImageStorageService.StoredImage;
import org.example.greenybackend.common.util.ImageDataUris;
import org.example.greenybackend.modules.user.UserAccountService;
import org.example.greenybackend.modules.user.AddressRepository;
import org.example.greenybackend.modules.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Address;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.dto.AddressRequest;
import org.example.greenybackend.modules.user.dto.AddressResponse;
import org.example.greenybackend.modules.user.dto.PasswordChangeRequest;
import org.example.greenybackend.modules.user.dto.UserProfileResponse;
import org.example.greenybackend.modules.user.dto.UserProfileUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    public UserAccountServiceImpl(
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordEncoder passwordEncoder,
            ImageStorageService imageStorageService
    ) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponse getProfile(UserEntity user) {
        return toProfile(requirePersistentUser(user));
    }

    @Transactional
    @Override
    public UserProfileResponse updateProfile(UserEntity user, UserProfileUpdateRequest request) {
        return updateProfile(user, request, null);
    }

    @Transactional
    @Override
    public UserProfileResponse updateProfile(UserEntity user, UserProfileUpdateRequest request, MultipartFile avatarFile) {
        UserEntity managedUser = requirePersistentUser(user);
        if (request == null) {
            throw new IllegalArgumentException("Du lieu cap nhat khong hop le");
        }
        String fullName = displayName(request.fullName(), request.title());
        if (fullName == null) {
            throw new IllegalArgumentException("Ho ten khong duoc de trong");
        }
        if (fullName.length() > 100) {
            throw new IllegalArgumentException("Ho ten khong duoc vuot qua 100 ky tu");
        }

        String phone = trimToNull(request.phone());
        validatePhone(phone, false);

        String email = normalizeEmail(request.email());
        if (email != null && !email.equalsIgnoreCase(managedUser.getEmail())) {
            validateEmail(email);
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getUserId().equals(managedUser.getUserId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Email da duoc su dung");
                    });
            managedUser.setEmail(email);
        }

        managedUser.setFullName(fullName);
        managedUser.setTitle(fullName);
        managedUser.setPhone(phone);
        applyAvatar(managedUser, imageStorageService.read(avatarFile));
        managedUser.setUpdateat(LocalDateTime.now());
        return toProfile(userRepository.save(managedUser));
    }

    @Transactional
    @Override
    public void changePassword(UserEntity user, PasswordChangeRequest request) {
        UserEntity managedUser = requirePersistentUser(user);
        if (request == null) {
            throw new IllegalArgumentException("Du lieu doi mat khau khong hop le");
        }
        if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), managedUser.getPass())) {
            throw new IllegalArgumentException("Mat khau hien tai khong dung");
        }
        if (request.newPassword() == null || request.newPassword().length() < 8) {
            throw new IllegalArgumentException("Mat khau moi phai co it nhat 8 ky tu");
        }
        if (passwordEncoder.matches(request.newPassword(), managedUser.getPass())) {
            throw new IllegalArgumentException("Mat khau moi khong duoc trung mat khau hien tai");
        }
        managedUser.setPass(passwordEncoder.encode(request.newPassword()));
        managedUser.setUpdateat(LocalDateTime.now());
        userRepository.save(managedUser);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AddressResponse> getAddresses(UserEntity user) {
        UserEntity managedUser = requirePersistentUser(user);
        return addressRepository.findByUserEntityUserIdOrderByIsDefaultDescCreatedAtDesc(managedUser.getUserId()).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    @Override
    public AddressResponse createAddress(UserEntity user, AddressRequest request) {
        UserEntity managedUser = requirePersistentUser(user);
        validateAddress(request);
        LocalDateTime now = LocalDateTime.now();

        Address address = new Address();
        address.setAddressId(UUID.randomUUID().toString());
        address.setUserEntity(managedUser);
        applyAddressRequest(address, request);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);

        boolean shouldBeDefault = Boolean.TRUE.equals(request.isDefault())
                || addressRepository.countByUserEntityUserId(managedUser.getUserId()) == 0;
        address.setIsDefault(shouldBeDefault);
        if (shouldBeDefault) {
            clearDefaultAddress(managedUser, null);
        }
        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    @Override
    public AddressResponse updateAddress(UserEntity user, String addressId, AddressRequest request) {
        UserEntity managedUser = requirePersistentUser(user);
        validateAddress(request);
        Address address = findUserAddress(managedUser, addressId);
        applyAddressRequest(address, request);
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultAddress(managedUser, address.getAddressId());
            address.setIsDefault(true);
        } else if (request.isDefault() != null) {
            address.setIsDefault(request.isDefault());
        }
        address.setUpdatedAt(LocalDateTime.now());
        ensureOneDefault(managedUser);
        return toAddressResponse(address);
    }

    @Transactional
    @Override
    public AddressResponse setDefaultAddress(UserEntity user, String addressId) {
        UserEntity managedUser = requirePersistentUser(user);
        Address address = findUserAddress(managedUser, addressId);
        clearDefaultAddress(managedUser, addressId);
        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        return toAddressResponse(address);
    }

    @Transactional
    @Override
    public void deleteAddress(UserEntity user, String addressId) {
        UserEntity managedUser = requirePersistentUser(user);
        Address address = findUserAddress(managedUser, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        if (wasDefault) {
            ensureOneDefault(managedUser);
        }
    }

    @Override
    public Address findUserAddress(UserEntity user, String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw new IllegalArgumentException("Can chon dia chi giao hang");
        }
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Can dang nhap de thuc hien thao tac");
        }
        return addressRepository.findByAddressIdAndUserEntityUserId(addressId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay dia chi giao hang"));
    }

    @Override
    public AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getAddressDetail(),
                address.getWardName(),
                address.getDistrictName(),
                address.getProvinceName(),
                formatAddress(address),
                address.getType(),
                address.getIsDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    private void applyAddressRequest(Address address, AddressRequest request) {
        address.setReceiverName(trimToNull(request.receiverName()));
        address.setReceiverPhone(trimToNull(request.receiverPhone()));
        address.setAddressDetail(trimToNull(request.addressDetail()));
        address.setWardName(trimToNull(request.wardName()));
        address.setDistrictName(trimToNull(request.districtName()));
        address.setProvinceName(trimToNull(request.provinceName()));
        address.setType(request.type() == null ? 0 : request.type());
        if (request.isDefault() != null) {
            address.setIsDefault(request.isDefault());
        }
    }

    private void validateAddress(AddressRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Du lieu dia chi khong hop le");
        }
        validateRequired(request.receiverName(), "Ten nguoi nhan khong duoc de trong");
        validateRequired(request.receiverPhone(), "So dien thoai nguoi nhan khong duoc de trong");
        validateRequired(request.addressDetail(), "Dia chi chi tiet khong duoc de trong");
        validateRequired(request.wardName(), "Phuong/xa khong duoc de trong");
        validateRequired(request.districtName(), "Quan/huyen khong duoc de trong");
        validateRequired(request.provinceName(), "Tinh/thanh pho khong duoc de trong");
        validatePhone(trimToNull(request.receiverPhone()), true);
        if (trimToNull(request.addressDetail()).length() > 50) {
            throw new IllegalArgumentException("Dia chi chi tiet khong duoc vuot qua 50 ky tu");
        }
    }

    private void clearDefaultAddress(UserEntity user, String exceptAddressId) {
        addressRepository.findByUserEntityUserIdOrderByIsDefaultDescCreatedAtDesc(user.getUserId()).stream()
                .filter(address -> exceptAddressId == null || !exceptAddressId.equals(address.getAddressId()))
                .forEach(address -> {
                    if (Boolean.TRUE.equals(address.getIsDefault())) {
                        address.setIsDefault(false);
                        address.setUpdatedAt(LocalDateTime.now());
                    }
                });
    }

    private void ensureOneDefault(UserEntity user) {
        List<Address> addresses = addressRepository.findByUserEntityUserIdOrderByIsDefaultDescCreatedAtDesc(user.getUserId());
        if (addresses.isEmpty() || addresses.stream().anyMatch(address -> Boolean.TRUE.equals(address.getIsDefault()))) {
            return;
        }
        Address first = addresses.stream()
                .max(Comparator.comparing(Address::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(addresses.get(0));
        first.setIsDefault(true);
        first.setUpdatedAt(LocalDateTime.now());
    }

    private UserProfileResponse toProfile(UserEntity user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getDisplayName(),
                user.getPhone(),
                ImageDataUris.userAvatar(user),
                user.getRole(),
                user.getStatus(),
                user.getCreateat(),
                user.getUpdateat()
        );
    }

    private UserEntity requirePersistentUser(UserEntity currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalArgumentException("Can dang nhap de thuc hien thao tac");
        }
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Token dang nhap khong hop le"));
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

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePhone(String phone, boolean required) {
        if (phone == null || phone.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("So dien thoai khong duoc de trong");
            }
            return;
        }
        if (!phone.matches("\\d{9,11}")) {
            throw new IllegalArgumentException("So dien thoai phai gom 9 den 11 chu so");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Email khong hop le");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void applyAvatar(UserEntity user, StoredImage image) {
        if (image == null) {
            return;
        }
        user.setAvatarData(image.data());
        user.setAvatarContentType(image.contentType());
        user.setAvatarFileName(image.fileName());
        user.setAvatarSize(image.size());
    }

    private String displayName(String fullName, String title) {
        String normalizedFullName = trimToNull(fullName);
        return normalizedFullName != null ? normalizedFullName : trimToNull(title);
    }
}
