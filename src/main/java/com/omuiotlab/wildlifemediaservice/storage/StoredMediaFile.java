package com.omuiotlab.wildlifemediaservice.storage;

import com.omuiotlab.wildlifemediaservice.model.MediaType;

public record StoredMediaFile(
        String originalFilename,
        String storedFilename,
        String storageKey,
        String contentType,
        long size,
        MediaType mediaType
) {
}