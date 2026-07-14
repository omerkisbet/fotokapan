package com.omuiotlab.wildlifemediaservice.service;

import com.omuiotlab.wildlifemediaservice.dto.MediaResponse;
import com.omuiotlab.wildlifemediaservice.exception.MediaNotFoundException;
import com.omuiotlab.wildlifemediaservice.model.MediaDocument;
import com.omuiotlab.wildlifemediaservice.model.MediaType;
import com.omuiotlab.wildlifemediaservice.repository.MediaRepository;
import com.omuiotlab.wildlifemediaservice.storage.MediaStorageService;
import com.omuiotlab.wildlifemediaservice.storage.StoredMediaFile;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final MediaStorageService mediaStorageService;
    private final CameraService cameraService;

    public MediaService(
            MediaRepository mediaRepository,
            MediaStorageService mediaStorageService,
            CameraService cameraService
    ) {
        this.mediaRepository = mediaRepository;
        this.mediaStorageService = mediaStorageService;
        this.cameraService = cameraService;
    }

    public MediaResponse upload(
            MultipartFile file,
            String cameraId,
            Instant capturedAt,
            String description
    ) {
        cameraService.getCamera(cameraId);
        StoredMediaFile storedFile = mediaStorageService.store(file);

        MediaDocument document = MediaDocument.builder()
                .originalFilename(storedFile.originalFilename())
                .storedFilename(storedFile.storedFilename())
                .storageKey(storedFile.storageKey())
                .contentType(storedFile.contentType())
                .size(storedFile.size())
                .mediaType(storedFile.mediaType())
                .cameraId(cameraId.trim().toUpperCase(java.util.Locale.ROOT))
                .capturedAt(capturedAt == null ? Instant.now() : capturedAt)
                .description(normalizeOptionalText(description))
                .build();

        try {
            return MediaResponse.from(mediaRepository.save(document));
        } catch (RuntimeException exception) {
            mediaStorageService.delete(storedFile.storageKey());
            throw exception;
        }
    }

    public Page<MediaResponse> list(
            MediaType mediaType,
            String cameraId,
            Pageable pageable,
            Authentication authentication
    ) {
        String normalizedCameraId = normalizeOptionalText(cameraId);
        Page<MediaDocument> result;

        if (normalizedCameraId != null) {
            cameraService.getAccessibleCamera(normalizedCameraId, authentication);
            result = mediaType == null
                    ? mediaRepository.findByCameraIdIgnoreCase(normalizedCameraId, pageable)
                    : mediaRepository.findByMediaTypeAndCameraIdIgnoreCase(
                            mediaType,
                            normalizedCameraId,
                            pageable
                    );
        } else if (cameraService.isAdmin(authentication)) {
            result = mediaType == null
                    ? mediaRepository.findAll(pageable)
                    : mediaRepository.findByMediaType(mediaType, pageable);
        } else {
            List<String> accessibleCodes = cameraService.accessibleCameraCodes(authentication);
            if (accessibleCodes.isEmpty()) {
                result = Page.empty(pageable);
            } else {
                result = mediaType == null
                        ? mediaRepository.findByCameraIdIn(accessibleCodes, pageable)
                        : mediaRepository.findByMediaTypeAndCameraIdIn(
                                mediaType,
                                accessibleCodes,
                                pageable
                        );
            }
        }

        return result.map(MediaResponse::from);
    }

    public MediaResponse getById(String id, Authentication authentication) {
        return MediaResponse.from(getAccessibleDocument(id, authentication));
    }

    public MediaDocument getAccessibleDocument(String id, Authentication authentication) {
        MediaDocument document = getDocument(id);
        cameraService.getAccessibleCamera(document.getCameraId(), authentication);
        return document;
    }

    public MediaDocument getDocument(String id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new MediaNotFoundException(id));
    }

    public Resource loadContent(String id, Authentication authentication) {
        MediaDocument document = getAccessibleDocument(id, authentication);
        return mediaStorageService.loadAsResource(document.getStorageKey());
    }

    public void delete(String id, Authentication authentication) {
        MediaDocument document = getAccessibleDocument(id, authentication);
        mediaStorageService.delete(document.getStorageKey());
        mediaRepository.delete(document);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
