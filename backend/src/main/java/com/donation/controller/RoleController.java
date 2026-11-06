package com.donation.controller;

import com.donation.dto.request.CreateRoleRequest;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.PermissionResponse;
import com.donation.dto.response.RoleResponse;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('roles.view')")
    public ResponseEntity<PageResponse<RoleResponse>> getRoles(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(roleService.getRoles(search, page, size));
    }

    @GetMapping("/roles/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('roles.view')")
    public ResponseEntity<RoleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('roles.create')")
    public ResponseEntity<RoleResponse> createRole(
        @Valid @RequestBody CreateRoleRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(201).body(roleService.createRole(request, principal));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('roles.update')")
    public ResponseEntity<RoleResponse> updateRole(
        @PathVariable Long id,
        @Valid @RequestBody CreateRoleRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(roleService.updateRole(id, request, principal));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('roles.update')")
    public ResponseEntity<Map<String, String>> updatePermissions(
        @PathVariable Long id,
        @RequestBody Map<String, List<Long>> body,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        roleService.updateRolePermissions(id, body.get("permissionIds"), principal);
        return ResponseEntity.ok(Map.of("message", "Permissions updated"));
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('permissions.view')")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id).getPermissions());
    }

    // Permissions endpoints
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permissions.view')")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }
}
