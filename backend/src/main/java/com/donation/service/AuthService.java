package com.donation.service;

import com.donation.dto.request.LoginRequest;
import com.donation.dto.request.ForgotPasswordRequest;
import com.donation.dto.request.ResetPasswordRequest;
import com.donation.dto.response.AuthResponse;
import com.donation.entity.PasswordResetToken;
import com.donation.entity.User;
import com.donation.exception.ApiException;
import com.donation.repository.PasswordResetTokenRepository;
import com.donation.repository.UserRepository;
import com.donation.security.CustomUserPrincipal;
import com.donation.security.JwtUtil;
import com.donation.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.password-reset.token-expiry-minutes}")
    private int resetTokenExpiryMinutes;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        cookieUtil.setAccessTokenCookie(response, accessToken);
        cookieUtil.setRefreshTokenCookie(response, refreshToken);

        auditService.log(user.getId(), "LOGIN", "USER", user.getId().toString());
        System.out.println(user);
        return buildAuthResponse(user, principal);
    }

    public void logout(CustomUserPrincipal principal, HttpServletResponse response) {
        cookieUtil.clearAuthCookies(response);
        auditService.log(principal.getUserId(), "LOGOUT", "USER", principal.getUserId().toString());
    }

    @Transactional
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.extractCookie(request, "refresh_token");

        if (refreshToken == null || !jwtUtil.isValidRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.unauthorized("User not found"));

        if (!user.isActive()) {
            throw ApiException.forbidden("Account is deactivated");
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, email);
        cookieUtil.setAccessTokenCookie(response, newAccessToken);

        // Build response manually to avoid loading lazy collections
        return AuthResponse.builder()
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent account enumeration
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Invalidate previous tokens
            passwordResetTokenRepository.deleteUnusedTokensByUserId(user.getId());

            // Generate secure random token
            SecureRandom secureRandom = new SecureRandom();
            byte[] tokenBytes = new byte[32];
            secureRandom.nextBytes(tokenBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            String tokenHash = CookieUtil.hashToken(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes))
                .build();
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw ApiException.badRequest("Passwords do not match");
        }

        String tokenHash = CookieUtil.hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link"));

        if (!resetToken.isValid()) {
            throw ApiException.badRequest("Reset link has expired or has already been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        userRepository.save(user);

        auditService.log(user.getId(), "PASSWORD_RESET", "USER", user.getId().toString());
    }

    public AuthResponse getMe(CustomUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> ApiException.notFound("User not found"));
        return buildAuthResponse(user, principal);
    }

    private AuthResponse buildAuthResponse(User user, CustomUserPrincipal principal) {
        List<String> roles = user.getRoles().stream()
            .map(r -> r.getName())
            .collect(Collectors.toList());

        Set<String> permissions = principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> !a.startsWith("ROLE_"))
            .collect(Collectors.toSet());

        List<AuthResponse.AreaInfo> areas = user.getAreas().stream()
            .map(a -> AuthResponse.AreaInfo.builder().id(a.getId()).name(a.getName()).build())
            .collect(Collectors.toList());

        return AuthResponse.builder()
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .roles(roles)
            .permissions(permissions)
            .areas(areas)
            .build();
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired password reset tokens");
    }
}
