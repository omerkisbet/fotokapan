package com.omuiotlab.wildlifemediaservice.storage;

import com.omuiotlab.wildlifemediaservice.exception.MediaStorageException;
import com.omuiotlab.wildlifemediaservice.model.MediaType;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4"
    );

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "video/mp4", ".mp4"
    );

    private final Path rootDirectory;

    public MediaStorageService(MediaStorageProperties properties) {
        this.rootDirectory = Path.of(properties.getStoragePath())
                .toAbsolutePath()
                .normalize();
    }

    @PostConstruct
    public void initializeStorage() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException exception) {
            throw new MediaStorageException(
                    "Medya depolama klasörü oluşturulamadı: " + rootDirectory,
                    exception
            );
        }
    }

    public StoredMediaFile store(MultipartFile file) {
        validateFile(file);

        String originalFilename = cleanOriginalFilename(file);
        String contentType = file.getContentType();
        MediaType mediaType = determineMediaType(contentType);

        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String storedFilename = UUID.randomUUID() + extension;

        LocalDate currentDate = LocalDate.now(ZoneOffset.UTC);

        String year = String.valueOf(currentDate.getYear());
        String month = String.format("%02d", currentDate.getMonthValue());

        Path relativeDirectory = Path.of(year, month);
        Path destinationDirectory = rootDirectory
                .resolve(relativeDirectory)
                .normalize();

        Path destinationFile = destinationDirectory
                .resolve(storedFilename)
                .normalize();

        ensureInsideStorage(destinationFile);

        try {
            Files.createDirectories(destinationDirectory);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        destinationFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            throw new MediaStorageException(
                    "Dosya depolama alanına kaydedilemedi: " + originalFilename,
                    exception
            );
        }

        String storageKey = relativeDirectory
                .resolve(storedFilename)
                .toString()
                .replace('\\', '/');

        return new StoredMediaFile(
                originalFilename,
                storedFilename,
                storageKey,
                contentType,
                file.getSize(),
                mediaType
        );
    }

    public Resource loadAsResource(String storageKey) {
        Path filePath = resolveStorageKey(storageKey);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new MediaStorageException(
                    "Medya dosyası bulunamadı: " + storageKey
            );
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.isReadable()) {
                throw new MediaStorageException(
                        "Medya dosyası okunamıyor: " + storageKey
                );
            }

            return resource;
        } catch (MalformedURLException exception) {
            throw new MediaStorageException(
                    "Geçersiz medya dosyası yolu: " + storageKey,
                    exception
            );
        }
    }

    public void delete(String storageKey) {
        Path filePath = resolveStorageKey(storageKey);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new MediaStorageException(
                    "Medya dosyası silinemedi: " + storageKey,
                    exception
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaStorageException("Yüklenecek dosya boş olamaz.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new MediaStorageException(
                    "Desteklenmeyen dosya türü. İzin verilen türler: JPG, PNG, WEBP ve MP4."
            );
        }
    }

    private String cleanOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new MediaStorageException("Dosya adı bulunamadı.");
        }

        String cleanedFilename = StringUtils.cleanPath(originalFilename);

        if (cleanedFilename.contains("..")) {
            throw new MediaStorageException(
                    "Dosya adında geçersiz yol ifadesi bulunuyor."
            );
        }

        return cleanedFilename;
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }

        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }

        throw new MediaStorageException(
                "Dosyanın medya türü belirlenemedi."
        );
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new MediaStorageException("Storage key boş olamaz.");
        }

        Path resolvedPath = rootDirectory
                .resolve(storageKey)
                .normalize();

        ensureInsideStorage(resolvedPath);

        return resolvedPath;
    }

    private void ensureInsideStorage(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new MediaStorageException(
                    "Depolama klasörü dışına erişim engellendi."
            );
        }
    }
}