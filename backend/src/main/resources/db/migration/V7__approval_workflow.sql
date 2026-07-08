-- Approval Workflow (Module 5): a review cycle on top of content_drafts.
-- Rejecting a draft returns it to DRAFT status, so a draft may accumulate
-- several approvals rows over its lifetime if resubmitted; only one
-- PENDING_APPROVAL row per content_id may exist at a time (partial unique index).

CREATE TABLE approvals (
    id             UUID PRIMARY KEY,
    content_id     UUID REFERENCES content_drafts (id) ON DELETE SET NULL,
    content_title  VARCHAR(200),
    content_type   VARCHAR(40),
    reviewer_id    UUID,
    status         VARCHAR(20)  NOT NULL,
    remarks        TEXT,
    approved_at    TIMESTAMPTZ,
    rejected_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID,
    updated_by     UUID
);

CREATE INDEX idx_approvals_content ON approvals (content_id);
CREATE INDEX idx_approvals_status ON approvals (status);
CREATE INDEX idx_approvals_created_at ON approvals (created_at DESC);
CREATE UNIQUE INDEX idx_approvals_one_pending_per_content
    ON approvals (content_id) WHERE status = 'PENDING_APPROVAL';

CREATE TABLE approval_history (
    id               UUID PRIMARY KEY,
    approval_id      UUID REFERENCES approvals (id) ON DELETE SET NULL,
    content_id       UUID,
    action           VARCHAR(20) NOT NULL,
    previous_status  VARCHAR(20),
    new_status       VARCHAR(20) NOT NULL,
    actor_id         UUID,
    actor_email      VARCHAR(255),
    remarks          TEXT,
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_approval_history_content ON approval_history (content_id);
CREATE INDEX idx_approval_history_approval ON approval_history (approval_id);
CREATE INDEX idx_approval_history_created_at ON approval_history (created_at DESC);

CREATE TABLE approval_comments (
    id            UUID PRIMARY KEY,
    approval_id   UUID NOT NULL REFERENCES approvals (id) ON DELETE CASCADE,
    content_id    UUID,
    author_id     UUID,
    author_email  VARCHAR(255),
    comment       TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_approval_comments_approval ON approval_comments (approval_id, created_at);
