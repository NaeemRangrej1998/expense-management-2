package com.donation.controller;

import com.donation.dto.request.CreateUserRequest;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.UserResponse;
import com.donation.enums.UserStatus;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('users.view')")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) UserStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.getUsers(search, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users.view')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users.create')")
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(201).body(userService.createUser(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users.update')")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('users.deactivate')")
    public ResponseEntity<Map<String, String>> updateStatus(
        @PathVariable Long id,
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserStatus status = UserStatus.valueOf(body.get("status").toUpperCase());
        userService.updateStatus(id, status, principal);
        return ResponseEntity.ok(Map.of("message", "User status updated"));
    }
}
