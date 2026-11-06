package com.donation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name too long")
    private String name;

    @Size(max = 500)
    private String description;

    private List<Long> permissionIds;
}
