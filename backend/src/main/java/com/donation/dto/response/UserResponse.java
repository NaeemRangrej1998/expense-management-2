package com.donation.dto.response;

import com.donation.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String mobileNumber;
    private String status;
    private List<RoleInfo> roles;
    private List<AreaInfo> areas;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class RoleInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class AreaInfo {
        private Long id;
        private String name;
    }

    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .mobileNumber(user.getMobileNumber())
            .status(user.getStatus().name())
            .roles(user.getRoles().stream()
                .map(r -> RoleInfo.builder().id(r.getId()).name(r.getName()).build())
                .collect(Collectors.toList()))
            .areas(user.getAreas().stream()
                .map(a -> AreaInfo.builder().id(a.getId()).name(a.getName()).build())
                .collect(Collectors.toList()))
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
