package com.donation.dto.request;

import com.donation.enums.UserStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name too long")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    @Size(max = 20, message = "Mobile number too long")
    private String mobileNumber;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Long roleId;

    private List<Long> areaIds;

    private UserStatus status = UserStatus.ACTIVE;
}
