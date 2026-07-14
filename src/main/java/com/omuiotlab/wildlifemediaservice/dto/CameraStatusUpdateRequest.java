package com.omuiotlab.wildlifemediaservice.dto;

import com.omuiotlab.wildlifemediaservice.model.CameraStatus;
import jakarta.validation.constraints.NotNull;

public record CameraStatusUpdateRequest(
        @NotNull(message = "Fotokapan durumu boş olamaz.")
        CameraStatus status
) {
}
