package com.omuiotlab.wildlifemediaservice.controller;

import com.omuiotlab.wildlifemediaservice.dto.CameraCreateRequest;
import com.omuiotlab.wildlifemediaservice.dto.CameraCustomerAssignRequest;
import com.omuiotlab.wildlifemediaservice.dto.CameraResponse;
import com.omuiotlab.wildlifemediaservice.dto.CameraStatusUpdateRequest;
import com.omuiotlab.wildlifemediaservice.service.CameraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraService cameraService;

    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @PostMapping
    public ResponseEntity<CameraResponse> create(
            @Valid @RequestBody CameraCreateRequest request
    ) {
        return ResponseEntity.status(201).body(cameraService.create(request));
    }

    @GetMapping
    public List<CameraResponse> list(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            Authentication authentication
    ) {
        return cameraService.list(activeOnly, authentication);
    }

    @GetMapping("/{cameraCode}")
    public CameraResponse getByCameraCode(
            @PathVariable String cameraCode,
            Authentication authentication
    ) {
        return cameraService.getByCameraCode(cameraCode, authentication);
    }

    @PatchMapping("/{cameraCode}/status")
    public CameraResponse updateStatus(
            @PathVariable String cameraCode,
            @Valid @RequestBody CameraStatusUpdateRequest request
    ) {
        return cameraService.updateStatus(cameraCode, request.status());
    }

    @PatchMapping("/{cameraCode}/customer")
    public CameraResponse assignCustomer(
            @PathVariable String cameraCode,
            @Valid @RequestBody CameraCustomerAssignRequest request
    ) {
        return cameraService.assignCustomer(cameraCode, request.customerId());
    }

    @DeleteMapping("/{cameraCode}")
    public ResponseEntity<Void> delete(@PathVariable String cameraCode) {
        cameraService.delete(cameraCode);
        return ResponseEntity.noContent().build();
    }
}
