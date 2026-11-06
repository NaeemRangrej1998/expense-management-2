package com.donation.controller;

import com.donation.dto.request.CreateAreaRequest;
import com.donation.dto.response.AreaResponse;
import com.donation.dto.response.PageResponse;
import com.donation.enums.AreaStatus;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.AreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @GetMapping
    @PreAuthorize("hasAuthority('areas.view')")
    public ResponseEntity<PageResponse<AreaResponse>> getAreas(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) AreaStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(areaService.getAreas(search, status, page, size));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AreaResponse>> getActiveAreas() {
        return ResponseEntity.ok(areaService.getActiveAreas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('areas.view')")
    public ResponseEntity<AreaResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('areas.create')")
    public ResponseEntity<AreaResponse> createArea(
        @Valid @RequestBody CreateAreaRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(201).body(areaService.createArea(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('areas.update')")
    public ResponseEntity<AreaResponse> updateArea(
        @PathVariable Long id,
        @Valid @RequestBody CreateAreaRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(areaService.updateArea(id, request, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('areas.deactivate')")
    public ResponseEntity<Map<String, String>> updateStatus(
        @PathVariable Long id,
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        AreaStatus status = AreaStatus.valueOf(body.get("status").toUpperCase());
        areaService.updateStatus(id, status, principal);
        return ResponseEntity.ok(Map.of("message", "Area status updated"));
    }
}
