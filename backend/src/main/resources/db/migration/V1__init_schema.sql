-- Foundation schema: cross-cutting audit log, notification, and application
-- settings tables. Primary keys are UUIDs generated application-side by
-- Hibernate, so no DB-side default/extension is required.

CREATE TABLE activity_log (
    id             UUID PRIMARY KEY,
    actor_id       UUID,
    actor_email    VARCHAR(255),
    action         VARCHAR(40)  NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      UUID,
    metadata       TEXT,
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_activity_log_entity ON activity_log (entity_type, entity_id);
CREATE INDEX idx_activity_log_actor ON activity_log (actor_id);
CREATE INDEX idx_activity_log_action ON activity_log (action);
CREATE INDEX idx_activity_log_created_at ON activity_log (created_at DESC);

CREATE TABLE notifications (
    id             UUID PRIMARY KEY,
    recipient_id   UUID,
    type           VARCHAR(40)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    message        TEXT         NOT NULL,
    link           VARCHAR(500),
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID,
    updated_by     UUID
);

CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);

CREATE TABLE app_settings (
    id             UUID PRIMARY KEY,
    setting_key    VARCHAR(150) NOT NULL UNIQUE,
    setting_value  TEXT,
    category       VARCHAR(40)  NOT NULL,
    description    VARCHAR(500),
    is_secret      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID,
    updated_by     UUID
);

CREATE INDEX idx_app_settings_category ON app_settings (category);
