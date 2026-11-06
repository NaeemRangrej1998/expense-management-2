package com.donation.util;

import com.donation.service.AuditService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge((int) (accessTokenExpiryMs / 1000));
        // SameSite must be set via header since Cookie API doesn't support it in older Servlet versions
        response.addCookie(cookie);
        response.addHeader("Set-Cookie",
            "access_token=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + (accessTokenExpiryMs / 1000));
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        response.addHeader("Set-Cookie",
            "refresh_token=" + token + "; Path=/api/auth/refresh; HttpOnly; SameSite=Strict; Max-Age=" + (refreshTokenExpiryMs / 1000));
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", "access_token=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
        response.addHeader("Set-Cookie", "refresh_token=; Path=/api/auth/refresh; HttpOnly; SameSite=Strict; Max-Age=0");
    }

    public String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
