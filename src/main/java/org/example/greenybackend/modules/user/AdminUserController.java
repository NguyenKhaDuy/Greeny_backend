package org.example.greenybackend.modules.user;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.dto.AdminUserResponse;
import org.example.greenybackend.modules.user.dto.AdminUserUpdateRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<AdminUserResponse> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate created
    ) {
        return userService.getAllUsers(name, email, phone, role, status, created);
    }

    @GetMapping("/{userId}")
    public AdminUserResponse getById(@PathVariable String userId) {
        return userService.getUser(userId);
    }

    @PostMapping("/{userId}")
    public AdminUserResponse update(
            @AuthenticationPrincipal(expression = "user") UserEntity currentAdmin,
            @PathVariable String userId,
            @RequestBody AdminUserUpdateRequest request
    ) {
        return userService.updateUser(userId, request, currentAdmin);
    }

    @PatchMapping("/{userId}/status")
    public AdminUserResponse setStatus(
            @AuthenticationPrincipal(expression = "user") UserEntity currentAdmin,
            @PathVariable String userId,
            @RequestParam Integer status
    ) {
        return userService.setStatus(userId, status, currentAdmin);
    }

    @DeleteMapping("/{userId}")
    public AdminUserResponse delete(
            @AuthenticationPrincipal(expression = "user") UserEntity currentAdmin,
            @PathVariable String userId
    ) {
        return userService.deleteUser(userId, currentAdmin);
    }
}
