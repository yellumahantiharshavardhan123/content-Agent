-- Content Draft Management: a curation/review layer on top of generated_content.
-- Drafts are soft-deleted (restorable) and independently editable once created.

CREATE TABLE content_drafts (
    id                    UUID PRIMARY KEY,
    generated_content_id  UUID REFERENCES generated_content (id) ON DELETE SET NULL,
    media_id              UUID REFERENCES media (id) ON DELETE SET NULL,
    content_type          VARCHAR(40)  NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    content_text          TEXT         NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_by            UUID,
    updated_by            UUID
);

CREATE INDEX idx_content_drafts_generated_content ON content_drafts (generated_content_id);
CREATE INDEX idx_content_drafts_status ON content_drafts (status);
CREATE INDEX idx_content_drafts_content_type ON content_drafts (content_type);
CREATE INDEX idx_content_drafts_created_at ON content_drafts (created_at DESC);
CREATE INDEX idx_content_drafts_deleted ON content_drafts (is_deleted);
