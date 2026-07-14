package com.omuiotlab.wildlifemediaservice.dto;

public record CsrfResponse(
        String headerName,
        String parameterName,
        String token
) {
}
