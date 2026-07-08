package com.arjunsports.contentagent.modules.draft;

/** Lifecycle state of a {@link ContentDraft} as it moves toward publication. */
public enum DraftStatus {
    DRAFT,
    READY_FOR_REVIEW,
    APPROVED,
    REJECTED,
    PUBLISHED,
    ARCHIVED
}
