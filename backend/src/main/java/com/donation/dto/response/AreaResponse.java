package com.donation.dto.response;

import com.donation.entity.Area;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AreaResponse {

    private Long id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AreaResponse from(Area area) {
        return AreaResponse.builder()
            .id(area.getId())
            .name(area.getName())
            .description(area.getDescription())
            .status(area.getStatus().name())
            .createdAt(area.getCreatedAt())
            .updatedAt(area.getUpdatedAt())
            .build();
    }
}
