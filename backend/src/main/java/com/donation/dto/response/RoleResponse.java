package com.donation.dto.response;

import com.donation.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private boolean systemRole;
    private List<PermissionResponse> permissions;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class PermissionInfo {
        private Long id;
        private String name;
        private String module;
        private String action;
    }

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
            .id(role.getId())
            .name(role.getName())
            .description(role.getDescription())
            .systemRole(role.isSystemRole())
            .permissions(role.getPermissions().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList()))
            .createdAt(role.getCreatedAt())
            .build();
    }
}
