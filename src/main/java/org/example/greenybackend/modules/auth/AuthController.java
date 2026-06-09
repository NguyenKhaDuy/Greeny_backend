package org.example.greenybackend.modules.auth;

import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.auth.dto.AuthResponse;
import org.example.greenybackend.modules.auth.dto.DeleteAccountConfirmRequest;
import org.example.greenybackend.modules.auth.dto.LoginRequest;
import org.example.greenybackend.modules.auth.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "GREENY_AUTH";

    private final AuthService authService;
    private final int authCookieMaxAgeSeconds;
    private final boolean authCookieSecure;
    private final String authCookieSameSite;

    public AuthController(
            AuthService authService,
            @Value("${greeny.auth.cookie-max-age-seconds:259200}") int authCookieMaxAgeSeconds,
            @Value("${greeny.auth.cookie-secure:false}") boolean authCookieSecure,
            @Value("${greeny.auth.cookie-same-site:Lax}") String authCookieSameSite
    ) {
        this.authService = authService;
        this.authCookieMaxAgeSeconds = authCookieMaxAgeSeconds;
        this.authCookieSecure = authCookieSecure;
        this.authCookieSameSite = authCookieSameSite;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Dang ky thanh cong. Vui long kiem tra email de xac thuc tai khoan."));
    }

    @GetMapping("/verify-email")
    public MessageResponse verifyEmail(@RequestParam String token) {
        authService.verifyRegisterEmail(token);
        return new MessageResponse("Xac thuc email thanh cong.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", buildAuthCookie(response.token(), authCookieMaxAgeSeconds).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser
    ) {
        authService.logout(currentUser);
        return ResponseEntity.ok()
                .header("Set-Cookie", buildAuthCookie("", 0).toString())
                .body(new MessageResponse("Dang xuat thanh cong."));
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal(expression = "user") UserEntity user) {
        return new AuthResponse(
                user.getUserId(),
                user.getEmail(),
                user.getTitle(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerat(),
                user.getReToken()
        );
    }

    @DeleteMapping("/account")
    public MessageResponse requestDeleteAccount(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser
    ) {
        authService.requestDeleteAccount(currentUser);
        return new MessageResponse("Da gui email xac thuc xoa tai khoan.");
    }

    @PostMapping("/account/delete-confirm")
    public MessageResponse confirmDeleteAccount(@RequestBody DeleteAccountConfirmRequest request) {
        authService.confirmDeleteAccount(request.token());
        return new MessageResponse("Xoa tai khoan thanh cong.");
    }

    @GetMapping("/account/delete-confirm")
    public MessageResponse confirmDeleteAccountByLink(@RequestParam String token) {
        authService.confirmDeleteAccount(token);
        return new MessageResponse("Xoa tai khoan thanh cong.");
    }

    private ResponseCookie buildAuthCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authCookieSecure)
                .sameSite(authCookieSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
