package com.donation.controller;

import com.donation.dto.request.ForgotPasswordRequest;
import com.donation.dto.request.LoginRequest;
import com.donation.dto.request.ResetPasswordRequest;
import com.donation.dto.response.AuthResponse;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        HttpServletResponse response
    ) {
        authService.logout(principal, response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.refreshToken(request, response));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(authService.getMe(principal));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
            "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
    }
}
