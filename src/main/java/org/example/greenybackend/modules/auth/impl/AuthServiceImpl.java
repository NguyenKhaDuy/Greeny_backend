package org.example.greenybackend.modules.auth.impl;

import org.example.greenybackend.modules.auth.AuthService;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.auth.dto.AuthResponse;
import org.example.greenybackend.modules.auth.dto.LoginRequest;
import org.example.greenybackend.modules.auth.dto.RegisterRequest;
import org.example.greenybackend.modules.notification.EmailService;
import org.example.greenybackend.modules.user.UserRepository;
import org.example.greenybackend.security.GreenyTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int STATUS_DELETED = -1;
    private static final int STATUS_PENDING_EMAIL = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int DEFAULT_ROLE = 1;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final GreenyTokenService tokenService;
    private final long emailVerificationExpirationMinutes;
    private final long deleteAccountExpirationMinutes;
    private final long authTokenExpirationSeconds;

    public AuthServiceImpl(
            UserRepository userRepository,
            EmailService emailService,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            GreenyTokenService tokenService,
            @Value("${greeny.auth.email-verification-expiration-minutes:30}") long emailVerificationExpirationMinutes,
            @Value("${greeny.auth.delete-account-expiration-minutes:30}") long deleteAccountExpirationMinutes,
            @Value("${greeny.auth.cookie-max-age-seconds:259200}") long authTokenExpirationSeconds
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailVerificationExpirationMinutes = emailVerificationExpirationMinutes;
        this.deleteAccountExpirationMinutes = deleteAccountExpirationMinutes;
        this.authTokenExpirationSeconds = authTokenExpirationSeconds;
    }

    @Transactional
    @Override
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateEmail(email);
        validatePassword(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email da duoc su dung");
        }

        LocalDateTime now = LocalDateTime.now();
        String fullName = displayName(request.fullName(), request.title());
        UserEntity user = new UserEntity();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPass(passwordEncoder.encode(request.password()));
        user.setFullName(fullName);
        user.setTitle(fullName);
        user.setPhone(trimToNull(request.phone()));
        user.setRole(DEFAULT_ROLE);
        user.setStatus(STATUS_PENDING_EMAIL);
        user.setCreateat(now);
        user.setUpdateat(now);
        String token = tokenService.createToken(
                GreenyTokenService.REGISTER_PREFIX,
                user,
                now,
                now.plusMinutes(emailVerificationExpirationMinutes)
        );
        user.setReToken(token);

        userRepository.save(user);
        emailService.sendRegisterVerification(email, token);
    }

    @Transactional
    @Override
    public void verifyRegisterEmail(String tokenValue) {
        UserEntity user = findValidTokenUser(
                tokenValue,
                GreenyTokenService.REGISTER_PREFIX,
                emailVerificationExpirationMinutes
        );
        LocalDateTime now = LocalDateTime.now();

        user.setStatus(STATUS_ACTIVE);
        user.setEmailVerat(now);
        user.setReToken(null);
        user.setUpdateat(now);
    }

    @Transactional
    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email hoac mat khau khong dung"));

        if (user.getStatus() == null || user.getStatus() == STATUS_PENDING_EMAIL) {
            throw new IllegalArgumentException("Tai khoan chua xac thuc email");
        }
        if (user.getStatus() == STATUS_DELETED) {
            throw new IllegalArgumentException("Tai khoan da bi xoa");
        }
        authenticate(email, request.password());

        LocalDateTime now = LocalDateTime.now();
        user.setLastlogin(now);
        user.setReToken(tokenService.createToken(
                GreenyTokenService.AUTH_PREFIX,
                user,
                now,
                now.plusSeconds(authTokenExpirationSeconds)
        ));
        user.setUpdateat(now);
        return toResponse(user);
    }

    @Transactional
    @Override
    public void logout(UserEntity currentUser) {
        UserEntity user = requirePersistentUser(currentUser);
        user.setReToken(null);
        user.setUpdateat(LocalDateTime.now());
    }

    @Transactional
    @Override
    public void requestDeleteAccount(UserEntity currentUser) {
        UserEntity user = requirePersistentUser(currentUser);
        if (user.getStatus() == STATUS_DELETED) {
            throw new IllegalArgumentException("Tai khoan da bi xoa");
        }

        LocalDateTime now = LocalDateTime.now();
        String token = tokenService.createToken(
                GreenyTokenService.DELETE_PREFIX,
                user,
                now,
                now.plusMinutes(deleteAccountExpirationMinutes)
        );
        user.setReToken(token);
        user.setUpdateat(now);
        emailService.sendDeleteAccountVerification(user.getEmail(), token);
    }

    @Transactional
    @Override
    public void confirmDeleteAccount(String tokenValue) {
        UserEntity user = findValidTokenUser(
                tokenValue,
                GreenyTokenService.DELETE_PREFIX,
                deleteAccountExpirationMinutes
        );
        LocalDateTime now = LocalDateTime.now();

        user.setStatus(STATUS_DELETED);
        user.setPass(null);
        user.setReToken(null);
        user.setUpdateat(now);
    }

    private UserEntity findValidTokenUser(String tokenValue, String prefix, long expirationMinutes) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Token xac thuc khong hop le");
        }
        if (!tokenValue.startsWith(prefix)) {
            throw new IllegalArgumentException("Token xac thuc khong hop le");
        }
        tokenService.verifySignedToken(tokenValue, prefix);

        UserEntity user = userRepository.findByReToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token xac thuc khong hop le"));

        LocalDateTime tokenCreatedAt = user.getUpdateat() == null ? user.getCreateat() : user.getUpdateat();
        if (tokenCreatedAt == null || tokenCreatedAt.plusMinutes(expirationMinutes).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token da het han");
        }
        return user;
    }

    private void authenticate(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Email hoac mat khau khong dung");
        }
    }

    private UserEntity requirePersistentUser(UserEntity currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalArgumentException("Can dang nhap de thuc hien thao tac");
        }
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Token dang nhap khong hop le"));
    }

    private AuthResponse toResponse(UserEntity user) {
        return new AuthResponse(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerat(),
                user.getReToken()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Email khong hop le");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Mat khau phai co it nhat 8 ky tu");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String displayName(String fullName, String title) {
        String normalizedFullName = trimToNull(fullName);
        return normalizedFullName != null ? normalizedFullName : trimToNull(title);
    }
}
