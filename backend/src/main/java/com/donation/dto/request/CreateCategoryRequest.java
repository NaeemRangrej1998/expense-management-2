package com.donation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name too long")
    private String name;

    @Size(max = 500, message = "Description too long")
    private String description;

    private String status; // ACTIVE or INACTIVE
}
