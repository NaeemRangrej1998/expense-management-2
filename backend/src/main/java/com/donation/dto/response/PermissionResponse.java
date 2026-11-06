package com.donation.dto.response;

import com.donation.entity.Permission;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {

    private Long id;
    private String name;
    private String description;
    private String module;
    private String action;

    public static PermissionResponse from(Permission p) {
        return PermissionResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .description(p.getDescription())
            .module(p.getModule())
            .action(p.getAction())
            .build();
    }
}
