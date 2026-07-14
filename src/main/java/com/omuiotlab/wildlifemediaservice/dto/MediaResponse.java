package com.omuiotlab.wildlifemediaservice.dto;

import com.omuiotlab.wildlifemediaservice.model.AnalysisStatus;
import com.omuiotlab.wildlifemediaservice.model.MediaDocument;
import com.omuiotlab.wildlifemediaservice.model.MediaType;

import java.time.Instant;

public record MediaResponse(
        String id,
        String originalFilename,
        String contentType,
        long size,
        MediaType mediaType,
        String cameraId,
        Instant capturedAt,
        String description,
        AnalysisStatus analysisStatus,
        String analysisResult,
        Instant createdAt,
        Instant updatedAt,
        String contentUrl,
        String downloadUrl
) {
    public static MediaResponse from(MediaDocument document) {
        String baseUrl = "/api/media/" + document.getId();

        return new MediaResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSize(),
                document.getMediaType(),
                document.getCameraId(),
                document.getCapturedAt(),
                document.getDescription(),
                document.getAnalysisStatus(),
                document.getAnalysisResult(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                baseUrl + "/content",
                baseUrl + "/download"
        );
    }
}
