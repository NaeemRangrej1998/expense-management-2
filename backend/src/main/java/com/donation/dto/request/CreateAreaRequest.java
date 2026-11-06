package com.donation.dto.request;

import com.donation.enums.AreaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAreaRequest {

    @NotBlank(message = "Area name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    private AreaStatus status = AreaStatus.ACTIVE;
}
