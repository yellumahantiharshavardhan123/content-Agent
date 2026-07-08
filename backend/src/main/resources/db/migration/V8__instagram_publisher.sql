-- Instagram Publisher (Module 6): connect a Business Account, publish approved
-- content to it (via the real Meta Graph API or a dev-only mock, chosen purely
-- by configuration), and keep an append-only history of every attempt.

CREATE TABLE instagram_accounts (
    id                      UUID PRIMARY KEY,
    business_account_id    VARCHAR(100) NOT NULL,
    facebook_page_id        VARCHAR(100),
    username                VARCHAR(150),
    access_token_encrypted TEXT         NOT NULL,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    connected_at            TIMESTAMPTZ  NOT NULL,
    disconnected_at        TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    created_by              UUID,
    updated_by              UUID
);

CREATE INDEX idx_instagram_accounts_active ON instagram_accounts (is_active);
-- At most one currently-connected account at a time.
CREATE UNIQUE INDEX idx_instagram_accounts_one_active ON instagram_accounts (is_active) WHERE is_active = TRUE;

CREATE TABLE instagram_posts (
    id                    UUID PRIMARY KEY,
    approval_id           UUID REFERENCES approvals (id) ON DELETE SET NULL,
    instagram_account_id UUID REFERENCES instagram_accounts (id) ON DELETE SET NULL,
    media_id              UUID REFERENCES media (id) ON DELETE SET NULL,
    caption               TEXT         NOT NULL,
    hashtags              TEXT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    instagram_media_id   VARCHAR(100),
    permalink             VARCHAR(500),
    publisher_name        VARCHAR(40),
    error_message         TEXT,
    published_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_by            UUID,
    updated_by            UUID
);

CREATE INDEX idx_instagram_posts_approval ON instagram_posts (approval_id);
CREATE INDEX idx_instagram_posts_account ON instagram_posts (instagram_account_id);
CREATE INDEX idx_instagram_posts_status ON instagram_posts (status);
CREATE INDEX idx_instagram_posts_created_at ON instagram_posts (created_at DESC);

CREATE TABLE instagram_publish_history (
    id                   UUID PRIMARY KEY,
    instagram_post_id   UUID REFERENCES instagram_posts (id) ON DELETE SET NULL,
    action               VARCHAR(20) NOT NULL,
    status               VARCHAR(20),
    publisher_name       VARCHAR(40),
    error_message        TEXT,
    actor_id             UUID,
    actor_email          VARCHAR(255),
    created_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_instagram_publish_history_post ON instagram_publish_history (instagram_post_id);
CREATE INDEX idx_instagram_publish_history_created_at ON instagram_publish_history (created_at DESC);
