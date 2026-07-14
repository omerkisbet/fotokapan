package com.omuiotlab.wildlifemediaservice.dto;

import com.omuiotlab.wildlifemediaservice.model.CameraStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CameraCreateRequest(
        @NotBlank(message = "Fotokapan kodu boş olamaz.")
        @Size(max = 80, message = "Fotokapan kodu en fazla 80 karakter olabilir.")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Fotokapan kodunda yalnızca harf, rakam, tire ve alt çizgi kullanılabilir."
        )
        String cameraCode,

        @NotBlank(message = "Fotokapan adı boş olamaz.")
        @Size(max = 120, message = "Fotokapan adı en fazla 120 karakter olabilir.")
        String name,

        @Size(max = 180, message = "Konum en fazla 180 karakter olabilir.")
        String location,

        @Size(max = 160, message = "Jitsi oda adı en fazla 160 karakter olabilir.")
        @Pattern(
                regexp = "^$|^[A-Za-z0-9_-]+$",
                message = "Jitsi oda adında yalnızca harf, rakam, tire ve alt çizgi kullanılabilir."
        )
        String jitsiRoomName,

        CameraStatus status,

        @Size(max = 100, message = "Müşteri kimliği en fazla 100 karakter olabilir.")
        String customerId,

        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
        String description,

        Boolean active
) {
}
