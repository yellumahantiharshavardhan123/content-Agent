-- Authentication & Authorization schema: users, refresh tokens (rotation +
-- invalidation), and password reset tokens. UUIDs are generated
-- application-side by Hibernate, consistent with V1.

CREATE TABLE users (
    id             UUID PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone          VARCHAR(20),
    password       VARCHAR(255) NOT NULL,
    role           VARCHAR(30)  NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID,
    updated_by     UUID
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE refresh_tokens (
    id                       UUID PRIMARY KEY,
    user_id                  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash               VARCHAR(128) NOT NULL UNIQUE,
    expires_at               TIMESTAMPTZ  NOT NULL,
    revoked                  BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at               TIMESTAMPTZ,
    replaced_by_token_hash   VARCHAR(128),
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    created_by               UUID,
    updated_by               UUID
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id, revoked);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);

CREATE TABLE password_reset_tokens (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash     VARCHAR(128) NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ  NOT NULL,
    used           BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID,
    updated_by     UUID
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
