package com.omuiotlab.wildlifemediaservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CameraCustomerAssignRequest(
        @NotBlank(message = "Müşteri kimliği boş olamaz.")
        String customerId
) {
}
