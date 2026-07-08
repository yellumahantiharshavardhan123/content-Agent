package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.modules.ai.ContentType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Composes the optional search/filter parameters accepted by {@code GET /api/drafts} into one query. */
final class DraftSpecifications {

    private DraftSpecifications() {
    }

    static Specification<ContentDraft> build(
            String search, DraftStatus status, ContentType contentType, UUID mediaId, boolean includeDeleted) {
        List<Specification<ContentDraft>> specs = new ArrayList<>();
        if (!includeDeleted) {
            specs.add(notDeleted());
        }
        specs.add(hasStatus(status));
        specs.add(hasContentType(contentType));
        specs.add(hasMediaId(mediaId));
        specs.add(searchText(search));

        return specs.stream()
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse(null);
    }

    private static Specification<ContentDraft> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    private static Specification<ContentDraft> hasStatus(DraftStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<ContentDraft> hasContentType(ContentType contentType) {
        if (contentType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("contentType"), contentType);
    }

    private static Specification<ContentDraft> hasMediaId(UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("mediaId"), mediaId);
    }

    private static Specification<ContentDraft> searchText(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("contentText")), pattern));
    }
}
