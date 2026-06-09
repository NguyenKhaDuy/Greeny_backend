package org.example.greenybackend.modules.user.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.dateEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.AdminUserService;
import org.example.greenybackend.modules.user.UserRepository;
import org.example.greenybackend.modules.user.dto.AdminUserResponse;
import org.example.greenybackend.modules.user.dto.AdminUserUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int ROLE_ADMIN = 0;
    private static final int ROLE_USER = 1;
    private static final int STATUS_DELETED = -1;
    private static final int STATUS_PENDING_EMAIL = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_LOCKED = 2;

    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdminUserResponse> getAllUsers() {
        return getAllUsers(null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdminUserResponse> getAllUsers(
            String name,
            String email,
            String phone,
            Integer role,
            Integer status,
            LocalDate created
    ) {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(UserEntity::getCreateat, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(user -> contains(user.getTitle(), name))
                .filter(user -> contains(user.getEmail(), email))
                .filter(user -> contains(user.getPhone(), phone))
                .filter(user -> role == null || role.equals(user.getRole()))
                .filter(user -> status == null || status.equals(user.getStatus()))
                .filter(user -> dateEquals(user.getCreateat(), created))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public AdminUserResponse getUser(String userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    @Override
    public AdminUserResponse updateUser(String userId, AdminUserUpdateRequest request, UserEntity currentAdmin) {
        UserEntity user = findUser(userId);
        boolean editingSelf = currentAdmin != null && userId.equals(currentAdmin.getUserId());

        if (request.role() != null) {
            validateRole(request.role());
            if (editingSelf && request.role() != ROLE_ADMIN) {
                throw new IllegalArgumentException("Không thể tự hạ quyền admin của chính mình");
            }
            user.setRole(request.role());
        }
        if (request.status() != null) {
            validateStatus(request.status());
            if (editingSelf && request.status() != STATUS_ACTIVE) {
                throw new IllegalArgumentException("Không thể tự khóa tài khoản đang đăng nhập");
            }
            user.setStatus(request.status());
            if (request.status() != STATUS_ACTIVE) {
                user.setReToken(null);
            }
        }

        user.setTitle(trimToNull(request.title()));
        user.setPhone(trimToNull(request.phone()));
        user.setUpdateat(LocalDateTime.now());
        return toResponse(user);
    }

    @Transactional
    @Override
    public AdminUserResponse setStatus(String userId, Integer status, UserEntity currentAdmin) {
        UserEntity user = findUser(userId);
        boolean editingSelf = currentAdmin != null && userId.equals(currentAdmin.getUserId());
        validateStatus(status);
        if (editingSelf && status != STATUS_ACTIVE) {
                throw new IllegalArgumentException("Không thể tự khóa tài khoản đang đăng nhập");
        }
        user.setStatus(status);
        if (status != STATUS_ACTIVE) {
            user.setReToken(null);
        }
        user.setUpdateat(LocalDateTime.now());
        return toResponse(user);
    }

    @Transactional
    @Override
    public AdminUserResponse deleteUser(String userId, UserEntity currentAdmin) {
        return setStatus(userId, STATUS_DELETED, currentAdmin);
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));
    }

    private void validateRole(Integer role) {
        if (role == null || (role != ROLE_ADMIN && role != ROLE_USER)) {
            throw new IllegalArgumentException("Quyền không hợp lệ");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != STATUS_DELETED && status != STATUS_PENDING_EMAIL
                && status != STATUS_ACTIVE && status != STATUS_LOCKED)) {
            throw new IllegalArgumentException("Trạng thái user không hợp lệ");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getTitle(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getRole(),
                roleLabel(user.getRole()),
                user.getStatus(),
                statusLabel(user.getStatus()),
                user.getEmailVerat(),
                user.getLastlogin(),
                user.getCreateat(),
                user.getUpdateat()
        );
    }

    private String roleLabel(Integer role) {
        if (role == null) {
            return "Chưa gán quyền";
        }
        return role == ROLE_ADMIN ? "Admin" : "User";
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "Không rõ";
        }
        return switch (status) {
            case STATUS_DELETED -> "Đã xóa";
            case STATUS_PENDING_EMAIL -> "Chờ xác thực";
            case STATUS_ACTIVE -> "Đang hoạt động";
            case STATUS_LOCKED -> "Đã khóa";
            default -> "Không rõ";
        };
    }
}
