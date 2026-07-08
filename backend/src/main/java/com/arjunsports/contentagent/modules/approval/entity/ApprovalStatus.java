package com.arjunsports.contentagent.modules.approval.entity;

/**
 * Lifecycle of an {@link Approval} request. An approval row is only ever
 * created at submission time (straight into {@code PENDING_APPROVAL}); the
 * {@code DRAFT} value exists for parity with the module's published state
 * diagram - it represents the underlying draft's resting state before any
 * approval request exists, or after a rejection sends it back, rather than a
 * state an approval row itself ever occupies.
 */
public enum ApprovalStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    READY_FOR_PUBLISH
}
