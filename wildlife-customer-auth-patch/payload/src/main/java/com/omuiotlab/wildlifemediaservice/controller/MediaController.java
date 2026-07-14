package com.omuiotlab.wildlifemediaservice.controller;

import com.omuiotlab.wildlifemediaservice.dto.MediaResponse;
import com.omuiotlab.wildlifemediaservice.model.MediaDocument;
import com.omuiotlab.wildlifemediaservice.model.MediaType;
import com.omuiotlab.wildlifemediaservice.service.MediaService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
@RequestMapping("/api/media")
@Validated
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam @NotBlank(message = "cameraId boş olamaz.") String cameraId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt,
            @RequestParam(required = false) String description
    ) {
        MediaResponse response = mediaService.upload(
                file,
                cameraId,
                capturedAt,
                description
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public Page<MediaResponse> list(
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) String cameraId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "capturedAt")
        );

        return mediaService.list(mediaType, cameraId, pageable, authentication);
    }

    @GetMapping("/{id}")
    public MediaResponse getById(
            @PathVariable String id,
            Authentication authentication
    ) {
        return mediaService.getById(id, authentication);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> view(
            @PathVariable String id,
            Authentication authentication
    ) {
        return buildResourceResponse(id, true, authentication);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String id,
            Authentication authentication
    ) {
        return buildResourceResponse(id, false, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            Authentication authentication
    ) {
        mediaService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> buildResourceResponse(
            String id,
            boolean inline,
            Authentication authentication
    ) {
        MediaDocument document = mediaService.getAccessibleDocument(id, authentication);
        Resource resource = mediaService.loadContent(id, authentication);

        ContentDisposition disposition = inline
                ? ContentDisposition.inline()
                    .filename(document.getOriginalFilename(), StandardCharsets.UTF_8)
                    .build()
                : ContentDisposition.attachment()
                    .filename(document.getOriginalFilename(), StandardCharsets.UTF_8)
                    .build();

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSize())
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
