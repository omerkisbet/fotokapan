package com.omuiotlab.wildlifemediaservice.storage;

import com.omuiotlab.wildlifemediaservice.exception.MediaStorageException;
import com.omuiotlab.wildlifemediaservice.model.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStorageServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAllowedImageAndLoadsItBack() throws Exception {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setStoragePath(temporaryDirectory.toString());
        MediaStorageService service = new MediaStorageService(properties);
        service.initializeStorage();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fox.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        StoredMediaFile stored = service.store(file);

        assertEquals("fox.jpg", stored.originalFilename());
        assertEquals(MediaType.IMAGE, stored.mediaType());
        assertTrue(Files.exists(temporaryDirectory.resolve(stored.storageKey())));
        assertTrue(service.loadAsResource(stored.storageKey()).isReadable());
    }

    @Test
    void rejectsUnsupportedContentType() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setStoragePath(temporaryDirectory.toString());
        MediaStorageService service = new MediaStorageService(properties);
        service.initializeStorage();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not-media".getBytes()
        );

        assertThrows(MediaStorageException.class, () -> service.store(file));
    }

    @Test
    void blocksAccessOutsideStorageDirectory() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setStoragePath(temporaryDirectory.toString());
        MediaStorageService service = new MediaStorageService(properties);
        service.initializeStorage();

        assertThrows(
                MediaStorageException.class,
                () -> service.loadAsResource("../secret.txt")
        );
    }
}
