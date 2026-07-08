package com.arjunsports.contentagent.modules.media;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.modules.media.dto.MediaResponse;
import com.arjunsports.contentagent.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofHours(1);

    private final MediaRepository mediaRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public MediaResponse upload(MultipartFile file, String description) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was provided");
        }

        String contentType = file.getContentType();
        MediaType mediaType = MediaValidationRules.resolveType(contentType);
        if (mediaType == null) {
            throw new BadRequestException(
                    "Unsupported file type '" + contentType + "'. Allowed: images (jpeg/png/webp/gif), "
                            + "videos (mp4/mov/mkv/webm), PDF, or plain text notes.");
        }

        long maxSize = MediaValidationRules.maxSizeBytes(mediaType);
        if (file.getSize() > maxSize) {
            throw new BadRequestException(
                    "File exceeds the maximum allowed size of " + (maxSize / (1024 * 1024)) + "MB for " + mediaType);
        }

        String storageKey = "media/" + UUID.randomUUID() + "/" + sanitizeFileName(file.getOriginalFilename());

        try {
            storageService.upload(storageKey, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded file", e);
        }

        Media media = Media.builder()
                .fileName(file.getOriginalFilename())
                .storageKey(storageKey)
                .contentType(contentType)
                .mediaType(mediaType)
                .fileSizeBytes(file.getSize())
                .description(description)
                .build();

        Media saved = mediaRepository.save(media);
        auditLogService.record(ActivityAction.UPLOAD, "Media", saved.getId(),
                Map.of("fileName", saved.getFileName(), "mediaType", saved.getMediaType().name()));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MediaResponse> list(MediaType mediaType, Pageable pageable) {
        Page<Media> page = mediaType != null
                ? mediaRepository.findByDeletedFalseAndMediaType(mediaType, pageable)
                : mediaRepository.findByDeletedFalse(pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Media media = findOrThrow(id);
        media.setDeleted(true);
        mediaRepository.save(media);
        storageService.delete(media.getStorageKey());
        auditLogService.record(ActivityAction.DELETE, "Media", media.getId());
    }

    private Media findOrThrow(UUID id) {
        return mediaRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Media", id));
    }

    private MediaResponse toResponse(Media media) {
        String url = storageService.presignedGetUrl(media.getStorageKey(), PRESIGNED_URL_TTL);
        return MediaResponse.from(media, url);
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file";
        }
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
