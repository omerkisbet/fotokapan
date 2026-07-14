package com.omuiotlab.wildlifemediaservice.model;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "media")

public class MediaDocument {
    @Id
    private String id;
    private String originalFilename;

    private String storedFilename;
    private String storageKey;
    private String contentType;
    private long size;
    private MediaType mediaType;
    private String cameraId;
    private Instant capturedAt;

    private String description;
    @Builder.Default
    private AnalysisStatus analysisStatus = AnalysisStatus.NOT_REQUESTED;
    private String analysisResult;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
