package com.donation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private List<String> roles;
    private Set<String> permissions;
    private List<AreaInfo> areas;

    @Data
    @Builder
    public static class AreaInfo {
        private Long id;
        private String name;
    }
}
