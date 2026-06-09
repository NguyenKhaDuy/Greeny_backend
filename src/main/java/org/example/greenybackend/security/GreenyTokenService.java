package org.example.greenybackend.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.example.greenybackend.domain.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GreenyTokenService {

    public static final String REGISTER_PREFIX = "REGISTER:";
    public static final String AUTH_PREFIX = "AUTH:";
    public static final String DELETE_PREFIX = "DELETE:";

    private final String tokenSigningSecret;

    public GreenyTokenService(
            @Value("${greeny.auth.token-signing-secret:greeny-dev-secret-change-me}") String tokenSigningSecret
    ) {
        this.tokenSigningSecret = tokenSigningSecret;
    }

    public String createToken(String prefix, UserEntity user, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        String tokenType = prefix.substring(0, prefix.length() - 1);
        String payload = "{"
                + "\"type\":\"" + jsonValue(tokenType) + "\","
                + "\"userId\":\"" + jsonValue(user.getUserId()) + "\","
                + "\"email\":\"" + jsonValue(user.getEmail()) + "\","
                + "\"role\":" + (user.getRole() == null ? "null" : user.getRole()) + ","
                + "\"issuedAt\":\"" + jsonValue(toUtcText(issuedAt)) + "\","
                + "\"expiresAt\":\"" + jsonValue(toUtcText(expiresAt)) + "\""
                + "}";
        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return prefix + encodedPayload + "." + sign(encodedPayload);
    }

    public void verifySignedToken(String tokenValue, String prefix) {
        if (tokenValue == null || tokenValue.isBlank() || !tokenValue.startsWith(prefix)) {
            throw new IllegalArgumentException("Token xac thuc khong hop le");
        }

        String unsignedToken = tokenValue.substring(prefix.length());
        int separatorIndex = unsignedToken.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == unsignedToken.length() - 1) {
            throw new IllegalArgumentException("Token xac thuc khong hop le");
        }

        String encodedPayload = unsignedToken.substring(0, separatorIndex);
        String signature = unsignedToken.substring(separatorIndex + 1);
        if (!sign(encodedPayload).equals(signature)) {
            throw new IllegalArgumentException("Token xac thuc khong hop le");
        }

        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String expectedType = "\"type\":\"" + prefix.substring(0, prefix.length() - 1) + "\"";
        if (!payload.contains(expectedType)) {
            throw new IllegalArgumentException("Token xac thuc khong hop le");
        }

        String expiresAt = extractJsonText(payload, "expiresAt");
        if (expiresAt == null || OffsetDateTime.parse(expiresAt).isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Token da het han");
        }
    }

    public boolean isAuthToken(String token) {
        return token != null && token.startsWith(AUTH_PREFIX);
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSigningSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Khong the ky token", exception);
        }
    }

    private String toUtcText(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toOffsetDateTime()
                .toString();
    }

    private String jsonValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJsonText(String payload, String fieldName) {
        String marker = "\"" + fieldName + "\":\"";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = payload.indexOf("\"", start);
        if (end < 0) {
            return null;
        }
        return payload.substring(start, end);
    }
}
