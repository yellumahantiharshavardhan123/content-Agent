-- Media Upload schema: uploaded images/videos/PDFs/text notes stored in MinIO,
-- with only the object key (not the binary) persisted here.

CREATE TABLE media (
    id                UUID PRIMARY KEY,
    file_name         VARCHAR(500)  NOT NULL,
    storage_key       VARCHAR(1000) NOT NULL UNIQUE,
    content_type      VARCHAR(150)  NOT NULL,
    media_type        VARCHAR(30)   NOT NULL,
    file_size_bytes   BIGINT        NOT NULL,
    description       TEXT,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    created_by        UUID,
    updated_by        UUID
);

CREATE INDEX idx_media_deleted_type ON media (deleted, media_type);
CREATE INDEX idx_media_created_at ON media (created_at DESC);
