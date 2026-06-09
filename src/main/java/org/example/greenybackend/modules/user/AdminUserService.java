package org.example.greenybackend.modules.user;

import java.time.LocalDate;
import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.dto.AdminUserResponse;
import org.example.greenybackend.modules.user.dto.AdminUserUpdateRequest;

public interface AdminUserService {

    List<AdminUserResponse> getAllUsers();

    List<AdminUserResponse> getAllUsers(
            String name,
            String email,
            String phone,
            Integer role,
            Integer status,
            LocalDate created
    );

    AdminUserResponse getUser(String userId);

    AdminUserResponse updateUser(String userId, AdminUserUpdateRequest request, UserEntity currentAdmin);

    AdminUserResponse setStatus(String userId, Integer status, UserEntity currentAdmin);

    AdminUserResponse deleteUser(String userId, UserEntity currentAdmin);

}
