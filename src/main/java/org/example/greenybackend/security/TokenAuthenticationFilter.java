package org.example.greenybackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDateTime;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_COOKIE_NAME = "GREENY_AUTH";
    private static final int STATUS_ACTIVE = 1;

    private final UserRepository userRepository;
    private final GreenyTokenService tokenService;
    private final long authTokenExpirationSeconds;

    public TokenAuthenticationFilter(
            UserRepository userRepository,
            GreenyTokenService tokenService,
            @Value("${greeny.auth.cookie-max-age-seconds:259200}") long authTokenExpirationSeconds
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.authTokenExpirationSeconds = authTokenExpirationSeconds;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            userRepository.findByReToken(token)
                    .filter(this::isActive)
                    .map(GreenyUserDetails::new)
                    .ifPresent(userDetails -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            return tokenService.isAuthToken(token) ? token : null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName()) && tokenService.isAuthToken(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isActive(UserEntity user) {
        return user.getStatus() != null
                && user.getStatus() == STATUS_ACTIVE
                && user.getLastlogin() != null
                && user.getLastlogin().plusSeconds(authTokenExpirationSeconds).isAfter(LocalDateTime.now());
    }
}
