package com.donation.service;

import com.donation.dto.request.CreateUserRequest;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.UserResponse;
import com.donation.entity.Area;
import com.donation.entity.Role;
import com.donation.entity.User;
import com.donation.enums.UserStatus;
import com.donation.exception.ApiException;
import com.donation.repository.AreaRepository;
import com.donation.repository.RoleRepository;
import com.donation.repository.UserRepository;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(String search, UserStatus status, int page, int size) {
        Page<User> result = userRepository.findAllWithFilters(
            search, status, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PageResponse.from(result.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User not found")));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, CustomUserPrincipal currentUser) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw ApiException.badRequest("Password is required when creating a user");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already registered");
        }

        Role role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> ApiException.notFound("Role not found"));

        // Prevent non-super-admins from creating super admins
        if (role.isSystemRole() && role.getName().equals("Super Admin") && !currentUser.isSuperAdmin()) {
            throw ApiException.forbidden("Cannot assign Super Admin role");
        }

        Set<Area> areas = new HashSet<>();
        if (request.getAreaIds() != null && !request.getAreaIds().isEmpty()) {
            List<Area> foundAreas = areaRepository.findAllById(request.getAreaIds());
            areas.addAll(foundAreas);
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .mobileNumber(request.getMobileNumber())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
            .roles(new HashSet<>(Set.of(role)))
            .areas(areas)
            .build();

        user = userRepository.save(user);

        auditService.log(currentUser.getUserId(), "CREATE_USER", "USER", user.getId().toString());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, CreateUserRequest request, CustomUserPrincipal currentUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User not found"));

        // Check email uniqueness
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already in use");
        }

        Role role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> ApiException.notFound("Role not found"));

        if (role.getName().equals("Super Admin") && !currentUser.isSuperAdmin()) {
            throw ApiException.forbidden("Cannot assign Super Admin role");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.getRoles().clear();
        user.getRoles().add(role);

        if (request.getAreaIds() != null) {
            user.getAreas().clear();
            user.getAreas().addAll(areaRepository.findAllById(request.getAreaIds()));
        }

        if (request.getStatus() != null) {
            // Prevent deactivating the last Super Admin
            if (UserStatus.INACTIVE.equals(request.getStatus())) {
                boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("Super Admin"));
                if (isSuperAdmin && userRepository.countActiveSuperAdmins() <= 1) {
                    throw ApiException.badRequest("Cannot deactivate the last active Super Admin");
                }
            }
            user.setStatus(request.getStatus());
        }

        user = userRepository.save(user);
        auditService.log(currentUser.getUserId(), "UPDATE_USER", "USER", id.toString());
        return UserResponse.from(user);
    }

    @Transactional
    public void updateStatus(Long id, UserStatus newStatus, CustomUserPrincipal currentUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User not found"));

        if (UserStatus.INACTIVE.equals(newStatus)) {
            boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("Super Admin"));
            if (isSuperAdmin && userRepository.countActiveSuperAdmins() <= 1) {
                throw ApiException.badRequest("Cannot deactivate the last active Super Admin");
            }
        }

        user.setStatus(newStatus);
        userRepository.save(user);
        auditService.log(currentUser.getUserId(), "UPDATE_USER_STATUS", "USER", id.toString());
    }
}
