-- AI Content Generation schema: versioned prompt templates, the editable
-- generated-content library, and an append-only generation history log.

CREATE TABLE prompt_templates (
    id                    UUID PRIMARY KEY,
    content_type          VARCHAR(40)  NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    description           VARCHAR(500),
    system_prompt         TEXT         NOT NULL,
    user_prompt_template  TEXT         NOT NULL,
    version               INTEGER      NOT NULL,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_by            UUID,
    updated_by            UUID
);

-- Exactly one active version per content type.
CREATE UNIQUE INDEX idx_prompt_templates_active_per_type
    ON prompt_templates (content_type) WHERE is_active = TRUE;
CREATE INDEX idx_prompt_templates_content_type ON prompt_templates (content_type);

CREATE TABLE generated_content (
    id                  UUID PRIMARY KEY,
    media_id            UUID REFERENCES media (id) ON DELETE SET NULL,
    content_type        VARCHAR(40)  NOT NULL,
    prompt_template_id  UUID REFERENCES prompt_templates (id) ON DELETE SET NULL,
    prompt_used         TEXT         NOT NULL,
    generated_text      TEXT,
    ai_model            VARCHAR(100),
    status              VARCHAR(20)  NOT NULL,
    error_message       TEXT,
    is_draft            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_edited           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    updated_by          UUID
);

CREATE INDEX idx_generated_content_media ON generated_content (media_id);
CREATE INDEX idx_generated_content_type ON generated_content (content_type);
CREATE INDEX idx_generated_content_created_at ON generated_content (created_at DESC);

CREATE TABLE generation_history (
    id                    UUID PRIMARY KEY,
    generated_content_id UUID REFERENCES generated_content (id) ON DELETE SET NULL,
    media_id              UUID,
    content_type          VARCHAR(40) NOT NULL,
    prompt_template_id    UUID,
    action                VARCHAR(20) NOT NULL,
    ai_model              VARCHAR(100),
    status                VARCHAR(20) NOT NULL,
    error_message         TEXT,
    latency_ms            INTEGER,
    actor_id              UUID,
    actor_email           VARCHAR(255),
    created_at            TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_generation_history_content ON generation_history (generated_content_id);
CREATE INDEX idx_generation_history_created_at ON generation_history (created_at DESC);
