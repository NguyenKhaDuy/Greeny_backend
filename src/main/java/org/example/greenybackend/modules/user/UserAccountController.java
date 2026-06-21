package org.example.greenybackend.modules.user;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.dto.AddressRequest;
import org.example.greenybackend.modules.user.dto.AddressResponse;
import org.example.greenybackend.modules.user.dto.PasswordChangeRequest;
import org.example.greenybackend.modules.user.dto.UserProfileResponse;
import org.example.greenybackend.modules.user.dto.UserProfileUpdateRequest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UserAccountController {

    private final UserAccountService accountService;

    public UserAccountController(UserAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return accountService.getProfile(currentUser);
    }

    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody UserProfileUpdateRequest request
    ) {
        return accountService.updateProfile(currentUser, request);
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse updateProfileMultipart(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) MultipartFile avatarFile
    ) {
        return accountService.updateProfile(currentUser, new UserProfileUpdateRequest(fullName, title, phone, email), avatarFile);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody PasswordChangeRequest request
    ) {
        accountService.changePassword(currentUser, request);
        return new MessageResponse("Doi mat khau thanh cong");
    }

    @GetMapping("/addresses")
    public List<AddressResponse> addresses(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return accountService.getAddresses(currentUser);
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> createAddress(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody AddressRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAddress(currentUser, request));
    }

    @PutMapping("/addresses/{addressId}")
    public AddressResponse updateAddress(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String addressId,
            @RequestBody AddressRequest request
    ) {
        return accountService.updateAddress(currentUser, addressId, request);
    }

    @PatchMapping("/addresses/{addressId}/default")
    public AddressResponse setDefaultAddress(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String addressId
    ) {
        return accountService.setDefaultAddress(currentUser, addressId);
    }

    @DeleteMapping("/addresses/{addressId}")
    public MessageResponse deleteAddress(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String addressId
    ) {
        accountService.deleteAddress(currentUser, addressId);
        return new MessageResponse("Xoa dia chi thanh cong");
    }
}
