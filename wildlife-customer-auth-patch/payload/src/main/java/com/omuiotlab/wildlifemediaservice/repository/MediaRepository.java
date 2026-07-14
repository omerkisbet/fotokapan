package com.omuiotlab.wildlifemediaservice.repository;

import com.omuiotlab.wildlifemediaservice.model.MediaDocument;
import com.omuiotlab.wildlifemediaservice.model.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface MediaRepository extends MongoRepository<MediaDocument, String> {

    Page<MediaDocument> findByMediaType(MediaType mediaType, Pageable pageable);

    Page<MediaDocument> findByCameraIdIgnoreCase(String cameraId, Pageable pageable);

    Page<MediaDocument> findByMediaTypeAndCameraIdIgnoreCase(
            MediaType mediaType,
            String cameraId,
            Pageable pageable
    );

    Page<MediaDocument> findByCameraIdIn(Collection<String> cameraIds, Pageable pageable);

    Page<MediaDocument> findByMediaTypeAndCameraIdIn(
            MediaType mediaType,
            Collection<String> cameraIds,
            Pageable pageable
    );

    boolean existsByStorageKey(String storageKey);
}
