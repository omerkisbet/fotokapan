package com.omuiotlab.wildlifemediaservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
        @NotBlank(message = "Müşteri adı boş olamaz.")
        @Size(max = 120, message = "Müşteri adı en fazla 120 karakter olabilir.")
        String fullName,

        @NotBlank(message = "E-posta adresi boş olamaz.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 180, message = "E-posta adresi en fazla 180 karakter olabilir.")
        String email,

        @NotBlank(message = "Parola boş olamaz.")
        @Size(min = 8, max = 72, message = "Parola 8 ile 72 karakter arasında olmalıdır.")
        String password
) {
}
