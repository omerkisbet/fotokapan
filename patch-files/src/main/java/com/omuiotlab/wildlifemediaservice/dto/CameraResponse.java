package com.omuiotlab.wildlifemediaservice.dto;

import com.omuiotlab.wildlifemediaservice.model.Camera;
import com.omuiotlab.wildlifemediaservice.model.CameraStatus;

import java.time.Instant;

public record CameraResponse(
        String id,
        String cameraCode,
        String name,
        String location,
        String jitsiRoomName,
        CameraStatus status,
        String customerId,
        String description,
        boolean active,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt,
        String liveUrl,
        String archiveUrl
) {
    public static CameraResponse from(Camera camera) {
        String encodedCameraCode = java.net.URLEncoder.encode(
                camera.getCameraCode(),
                java.nio.charset.StandardCharsets.UTF_8
        );

        return new CameraResponse(
                camera.getId(),
                camera.getCameraCode(),
                camera.getName(),
                camera.getLocation(),
                camera.getJitsiRoomName(),
                camera.getStatus(),
                camera.getCustomerId(),
                camera.getDescription(),
                camera.isActive(),
                camera.getLastSeenAt(),
                camera.getCreatedAt(),
                camera.getUpdatedAt(),
                "/live.html?camera=" + encodedCameraCode + "&mode=viewer",
                "/?cameraCode=" + encodedCameraCode
        );
    }
}
