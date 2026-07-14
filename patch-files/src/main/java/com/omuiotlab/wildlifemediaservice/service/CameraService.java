package com.omuiotlab.wildlifemediaservice.service;

import com.omuiotlab.wildlifemediaservice.dto.CameraCreateRequest;
import com.omuiotlab.wildlifemediaservice.dto.CameraResponse;
import com.omuiotlab.wildlifemediaservice.exception.CameraNotFoundException;
import com.omuiotlab.wildlifemediaservice.exception.DuplicateCameraCodeException;
import com.omuiotlab.wildlifemediaservice.model.Camera;
import com.omuiotlab.wildlifemediaservice.model.CameraStatus;
import com.omuiotlab.wildlifemediaservice.repository.CameraRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class CameraService {

    private static final String DEFAULT_ROOM_PREFIX = "OMUIoTLabWildlife_";

    private final CameraRepository cameraRepository;

    public CameraService(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    public CameraResponse create(CameraCreateRequest request) {
        String cameraCode = normalizeCameraCode(request.cameraCode());

        if (cameraRepository.existsByCameraCodeIgnoreCase(cameraCode)) {
            throw new DuplicateCameraCodeException(cameraCode);
        }

        CameraStatus status = request.status() == null
                ? CameraStatus.OFFLINE
                : request.status();

        Camera camera = Camera.builder()
                .cameraCode(cameraCode)
                .name(request.name().trim())
                .location(normalizeOptionalText(request.location()))
                .jitsiRoomName(resolveRoomName(request.jitsiRoomName(), cameraCode))
                .status(status)
                .customerId(normalizeOptionalText(request.customerId()))
                .description(normalizeOptionalText(request.description()))
                .active(request.active() == null || request.active())
                .lastSeenAt(status == CameraStatus.ONLINE ? Instant.now() : null)
                .build();

        return CameraResponse.from(cameraRepository.save(camera));
    }

    public List<CameraResponse> list(boolean activeOnly) {
        List<Camera> cameras = activeOnly
                ? cameraRepository.findByActiveTrueOrderByNameAsc()
                : cameraRepository.findAllByOrderByNameAsc();

        return cameras.stream()
                .map(CameraResponse::from)
                .toList();
    }

    public CameraResponse getByCameraCode(String cameraCode) {
        return CameraResponse.from(getCamera(cameraCode));
    }

    public CameraResponse updateStatus(String cameraCode, CameraStatus status) {
        Camera camera = getCamera(cameraCode);
        camera.setStatus(status);

        if (status == CameraStatus.ONLINE) {
            camera.setLastSeenAt(Instant.now());
        }

        return CameraResponse.from(cameraRepository.save(camera));
    }

    public void delete(String cameraCode) {
        cameraRepository.delete(getCamera(cameraCode));
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
