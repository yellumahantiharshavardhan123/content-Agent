package com.arjunsports.contentagent.modules.media.dto;

import com.arjunsports.contentagent.modules.media.Media;
import com.arjunsports.contentagent.modules.media.MediaType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MediaResponse(
        UUID id,
        String fileName,
        MediaType mediaType,
        String contentType,
        long fileSizeBytes,
        String description,
        String url,
        Instant createdAt) {

    public static MediaResponse from(Media media, String url) {
        return MediaResponse.builder()
                .id(media.getId())
                .fileName(media.getFileName())
                .mediaType(media.getMediaType())
                .contentType(media.getContentType())
                .fileSizeBytes(media.getFileSizeBytes())
                .description(media.getDescription())
                .url(url)
                .createdAt(media.getCreatedAt())
                .build();
    }
}
