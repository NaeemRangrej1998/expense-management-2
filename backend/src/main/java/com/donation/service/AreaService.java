package com.donation.service;

import com.donation.dto.request.CreateAreaRequest;
import com.donation.dto.response.AreaResponse;
import com.donation.dto.response.PageResponse;
import com.donation.entity.Area;
import com.donation.enums.AreaStatus;
import com.donation.exception.ApiException;
import com.donation.repository.AreaRepository;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<AreaResponse> getAreas(String search, AreaStatus status, int page, int size) {
        Page<Area> result = areaRepository.findAllWithFilters(
            search, status, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PageResponse.from(result.map(AreaResponse::from));
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> getActiveAreas() {
        return areaRepository.findByStatus(AreaStatus.ACTIVE)
            .stream().map(AreaResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AreaResponse getById(Long id) {
        return AreaResponse.from(areaRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Area not found")));
    }

    @Transactional
    public AreaResponse createArea(CreateAreaRequest request, CustomUserPrincipal currentUser) {
        if (areaRepository.existsByName(request.getName())) {
            throw ApiException.conflict("Area name already exists");
        }

        Area area = Area.builder()
            .name(request.getName())
            .description(request.getDescription())
            .status(request.getStatus() != null ? request.getStatus() : AreaStatus.ACTIVE)
            .build();

        area = areaRepository.save(area);
        auditService.log(currentUser.getUserId(), "CREATE_AREA", "AREA", area.getId().toString());
        return AreaResponse.from(area);
    }

    @Transactional
    public AreaResponse updateArea(Long id, CreateAreaRequest request, CustomUserPrincipal currentUser) {
        Area area = areaRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Area not found"));

        if (areaRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ApiException.conflict("Area name already exists");
        }

        area.setName(request.getName());
        area.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            area.setStatus(request.getStatus());
        }

        area = areaRepository.save(area);
        auditService.log(currentUser.getUserId(), "UPDATE_AREA", "AREA", id.toString());
        return AreaResponse.from(area);
    }

    @Transactional
    public void updateStatus(Long id, AreaStatus status, CustomUserPrincipal currentUser) {
        Area area = areaRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Area not found"));
        area.setStatus(status);
        areaRepository.save(area);
        auditService.log(currentUser.getUserId(), "UPDATE_AREA_STATUS", "AREA", id.toString());
    }
}
