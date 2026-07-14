package com.omuiotlab.wildlifemediaservice.service;

import com.omuiotlab.wildlifemediaservice.dto.CameraCreateRequest;
import com.omuiotlab.wildlifemediaservice.dto.CameraResponse;
import com.omuiotlab.wildlifemediaservice.exception.CameraNotFoundException;
import com.omuiotlab.wildlifemediaservice.exception.DuplicateCameraCodeException;
import com.omuiotlab.wildlifemediaservice.model.AppUser;
import com.omuiotlab.wildlifemediaservice.model.Camera;
import com.omuiotlab.wildlifemediaservice.model.CameraStatus;
import com.omuiotlab.wildlifemediaservice.repository.CameraRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class CameraService {

    private static final String DEFAULT_ROOM_PREFIX = "OMUIoTLabWildlife_";

    private final CameraRepository cameraRepository;
    private final AppUserService appUserService;

    public CameraService(
            CameraRepository cameraRepository,
            AppUserService appUserService
    ) {
        this.cameraRepository = cameraRepository;
        this.appUserService = appUserService;
    }

    public CameraResponse create(CameraCreateRequest request) {
        String cameraCode = normalizeCameraCode(request.cameraCode());

        if (cameraRepository.existsByCameraCodeIgnoreCase(cameraCode)) {
            throw new DuplicateCameraCodeException(cameraCode);
        }

        CameraStatus status = request.status() == null
                ? CameraStatus.OFFLINE
                : request.status();

        String customerId = normalizeOptionalText(request.customerId());
        if (customerId != null) {
            appUserService.getCustomerById(customerId);
        }

        Camera camera = Camera.builder()
                .cameraCode(cameraCode)
                .name(request.name().trim())
                .location(normalizeOptionalText(request.location()))
                .jitsiRoomName(resolveRoomName(request.jitsiRoomName(), cameraCode))
                .status(status)
                .customerId(customerId)
                .description(normalizeOptionalText(request.description()))
                .active(request.active() == null || request.active())
                .lastSeenAt(status == CameraStatus.ONLINE ? Instant.now() : null)
                .build();

        return CameraResponse.from(cameraRepository.save(camera));
    }

    public List<CameraResponse> list(boolean activeOnly, Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        List<Camera> cameras;

        if (appUserService.isAdmin(currentUser)) {
            cameras = activeOnly
                    ? cameraRepository.findByActiveTrueOrderByNameAsc()
                    : cameraRepository.findAllByOrderByNameAsc();
        } else {
            cameras = activeOnly
                    ? cameraRepository.findByCustomerIdAndActiveTrueOrderByNameAsc(currentUser.getId())
                    : cameraRepository.findByCustomerIdOrderByNameAsc(currentUser.getId());
        }

        return cameras.stream()
                .map(CameraResponse::from)
                .toList();
    }

    public CameraResponse getByCameraCode(
            String cameraCode,
            Authentication authentication
    ) {
        return CameraResponse.from(getAccessibleCamera(cameraCode, authentication));
    }

    public CameraResponse updateStatus(String cameraCode, CameraStatus status) {
        Camera camera = getCamera(cameraCode);
        camera.setStatus(status);

        if (status == CameraStatus.ONLINE) {
            camera.setLastSeenAt(Instant.now());
        }

        return CameraResponse.from(cameraRepository.save(camera));
    }

    public CameraResponse assignCustomer(String cameraCode, String customerId) {
        Camera camera = getCamera(cameraCode);
        AppUser customer = appUserService.getCustomerById(customerId);
        camera.setCustomerId(customer.getId());
        return CameraResponse.from(cameraRepository.save(camera));
    }

    public void delete(String cameraCode) {
        cameraRepository.delete(getCamera(cameraCode));
    }

    public Camera getAccessibleCamera(
            String cameraCode,
            Authentication authentication
    ) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        Camera camera = getCamera(cameraCode);

        if (appUserService.isAdmin(currentUser)) {
            return camera;
        }

        if (camera.getCustomerId() == null || !camera.getCustomerId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bu fotokapana erişim yetkiniz yok.");
        }

        return camera;
    }

    public List<String> accessibleCameraCodes(Authentication authentication) {
        AppUser currentUser = appUserService.getCurrentUser(authentication);
        List<Camera> cameras = appUserService.isAdmin(currentUser)
                ? cameraRepository.findAllByOrderByNameAsc()
                : cameraRepository.findByCustomerIdOrderByNameAsc(currentUser.getId());

        return cameras.stream()
                .map(Camera::getCameraCode)
                .toList();
    }

    public boolean isAdmin(Authentication authentication) {
        return appUserService.isAdmin(appUserService.getCurrentUser(authentication));
    }

    public Camera getCamera(String cameraCode) {
        String normalizedCode = normalizeCameraCode(cameraCode);
        return cameraRepository.findByCameraCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new CameraNotFoundException(normalizedCode));
    }

    private String normalizeCameraCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fotokapan kodu boş olamaz.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveRoomName(String requestedRoomName, String cameraCode) {
        String normalizedRoomName = normalizeOptionalText(requestedRoomName);
        if (normalizedRoomName != null) {
            return normalizedRoomName;
        }

        String safeCode = cameraCode.replaceAll("[^A-Za-z0-9]", "");
        return DEFAULT_ROOM_PREFIX + safeCode;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
