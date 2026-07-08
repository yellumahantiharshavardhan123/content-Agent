package com.arjunsports.contentagent.modules.media;

import java.util.Map;
import java.util.Set;

/** Per-type upload constraints: allowed MIME types and maximum file size. */
final class MediaValidationRules {

    private static final Map<MediaType, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            MediaType.IMAGE, Set.of("image/jpeg", "image/png", "image/webp", "image/gif"),
            MediaType.VIDEO, Set.of("video/mp4", "video/quicktime", "video/x-matroska", "video/webm"),
            MediaType.PDF, Set.of("application/pdf"),
            MediaType.TEXT_NOTE, Set.of("text/plain"));

    private static final Map<MediaType, Long> MAX_SIZE_BYTES = Map.of(
            MediaType.IMAGE, 10L * 1024 * 1024,
            MediaType.VIDEO, 200L * 1024 * 1024,
            MediaType.PDF, 20L * 1024 * 1024,
            MediaType.TEXT_NOTE, 2L * 1024 * 1024);

    private MediaValidationRules() {
    }

    static MediaType resolveType(String contentType) {
        return ALLOWED_CONTENT_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().contains(contentType))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    static boolean isAllowed(MediaType mediaType, String contentType) {
        return ALLOWED_CONTENT_TYPES.getOrDefault(mediaType, Set.of()).contains(contentType);
    }

    static long maxSizeBytes(MediaType mediaType) {
        return MAX_SIZE_BYTES.getOrDefault(mediaType, 10L * 1024 * 1024);
    }

    static String allowedTypesDescription(MediaType mediaType) {
        return String.join(", ", ALLOWED_CONTENT_TYPES.getOrDefault(mediaType, Set.of()));
    }
}
