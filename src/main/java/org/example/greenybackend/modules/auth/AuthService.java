package org.example.greenybackend.modules.auth;

import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.auth.dto.AuthResponse;
import org.example.greenybackend.modules.auth.dto.LoginRequest;
import org.example.greenybackend.modules.auth.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    void verifyRegisterEmail(String tokenValue);

    AuthResponse login(LoginRequest request);

    void logout(UserEntity currentUser);

    void requestDeleteAccount(UserEntity currentUser);

    void confirmDeleteAccount(String tokenValue);

}
