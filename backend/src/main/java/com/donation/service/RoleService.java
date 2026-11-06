package com.donation.service;

import com.donation.dto.request.CreateRoleRequest;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.PermissionResponse;
import com.donation.dto.response.RoleResponse;
import com.donation.entity.Permission;
import com.donation.entity.Role;
import com.donation.exception.ApiException;
import com.donation.repository.PermissionRepository;
import com.donation.repository.RoleRepository;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getRoles(String search, int page, int size) {
        Page<Role> result = roleRepository.findAllWithSearch(
            search, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PageResponse.from(result.map(RoleResponse::from));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(RoleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return RoleResponse.from(roleRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Role not found")));
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, CustomUserPrincipal currentUser) {
        if (roleRepository.existsByName(request.getName())) {
            throw ApiException.conflict("Role name already exists");
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findByIdIn(new HashSet<>(request.getPermissionIds())));
        }

        Role role = Role.builder()
            .name(request.getName())
            .description(request.getDescription())
            .systemRole(false)
            .permissions(permissions)
            .build();

        role = roleRepository.save(role);
        auditService.log(currentUser.getUserId(), "CREATE_ROLE", "ROLE", role.getId().toString());
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, CreateRoleRequest request, CustomUserPrincipal currentUser) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Role not found"));

        if (role.isSystemRole()) {
            // Allow updating permissions but not name for system roles
            if (!role.getName().equals(request.getName())) {
                throw ApiException.badRequest("Cannot rename a system role");
            }
        }

        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw ApiException.conflict("Role name already exists");
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(
                permissionRepository.findByIdIn(new HashSet<>(request.getPermissionIds()))
            );
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
        }

        role = roleRepository.save(role);
        auditService.log(currentUser.getUserId(), "UPDATE_ROLE", "ROLE", id.toString());
        return RoleResponse.from(role);
    }

    @Transactional
    public void updateRolePermissions(Long id, List<Long> permissionIds, CustomUserPrincipal currentUser) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Role not found"));

        Set<Permission> permissions = new HashSet<>(
            permissionRepository.findByIdIn(new HashSet<>(permissionIds))
        );
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
        auditService.log(currentUser.getUserId(), "UPDATE_ROLE_PERMISSIONS", "ROLE", id.toString());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscNameAsc()
            .stream().map(PermissionResponse::from).collect(Collectors.toList());
    }
}
