# Database Design — Arjun Sports AI Content Agent

**Engine:** PostgreSQL 16. **Schema ownership:** Flyway migrations only (`spring.jpa.hibernate.ddl-auto: validate` — Hibernate never generates or alters DDL, it only verifies entity mappings match what Flyway created). **Primary keys:** every table uses a `UUID` generated application-side by Hibernate (`@UuidGenerator`), so no table relies on a database-side default or the `pgcrypto`/`uuid-ossp` extensions.

This document covers exactly the six tables that exist after migrations `V1` and `V2`. No table described here is speculative.

---

## 1. Entity-Relationship Diagram (ASCII)

```
┌───────────────────────────┐
│         users              │
├───────────────────────────┤
│ PK id               UUID   │
│    first_name       VARCHAR│
│    last_name        VARCHAR│
│    email      UNIQUE VARCHAR│
│    phone            VARCHAR│
│    password         VARCHAR│
│    role             VARCHAR│
│    status           VARCHAR│
│    is_active        BOOLEAN│
│    last_login    TIMESTAMPTZ│
│    created_at    TIMESTAMPTZ│
│    updated_at    TIMESTAMPTZ│
│    created_by       UUID   │
│    updated_by       UUID   │
└─────────────┬─────────────┘
              │ 1
              │
              │ N                          N
              ├─────────────────────┬───────────────────────┐
              ▼                     ▼                       │
┌───────────────────────────┐ ┌───────────────────────────┐  │
│      refresh_tokens        │ │   password_reset_tokens    │  │
├───────────────────────────┤ ├───────────────────────────┤  │
│ PK id               UUID   │ │ PK id               UUID   │  │
│ FK user_id ─────────UUID   │ │ FK user_id ─────────UUID   │  │
│    token_hash UNIQUE VARCHAR│ │    token_hash UNIQUE VARCHAR│  │
│    expires_at   TIMESTAMPTZ│ │    expires_at   TIMESTAMPTZ│  │
│    revoked          BOOLEAN│ │    used             BOOLEAN│  │
│    revoked_at   TIMESTAMPTZ│ │    used_at      TIMESTAMPTZ│  │
│    replaced_by_token_hash  │ │    created_at   TIMESTAMPTZ│  │
│    created_at   TIMESTAMPTZ│ │    updated_at   TIMESTAMPTZ│  │
│    updated_at   TIMESTAMPTZ│ │    created_by       UUID   │  │
│    created_by       UUID   │ │    updated_by       UUID   │  │
│    updated_by       UUID   │ └───────────────────────────┘  │
└───────────────────────────┘                                 │
                                                                │
   ON DELETE CASCADE on both user_id foreign keys ──────────────┘

┌───────────────────────────┐   ┌───────────────────────────┐   ┌───────────────────────────┐
│       activity_log          │   │       notifications        │   │       app_settings          │
├───────────────────────────┤   ├───────────────────────────┤   ├───────────────────────────┤
│ PK id               UUID   │   │ PK id               UUID   │   │ PK id               UUID   │
│    actor_id         UUID   │   │    recipient_id     UUID   │   │    setting_key UNIQUE VARCHAR│
│    actor_email      VARCHAR│   │    type             VARCHAR│   │    setting_value      TEXT  │
│    action           VARCHAR│   │    title            VARCHAR│   │    category         VARCHAR│
│    entity_type      VARCHAR│   │    message             TEXT│   │    description      VARCHAR│
│    entity_id        UUID   │   │    link             VARCHAR│   │    is_secret        BOOLEAN│
│    metadata            TEXT│   │    is_read          BOOLEAN│   │    created_at   TIMESTAMPTZ│
│    created_at   TIMESTAMPTZ│   │    read_at      TIMESTAMPTZ│   │    updated_at   TIMESTAMPTZ│
│  (no FK - actor_id/entity_id│   │    created_at   TIMESTAMPTZ│   │    created_by       UUID   │
│   are denormalized, not     │   │    updated_at   TIMESTAMPTZ│   │    updated_by       UUID   │
│   relational references)    │   │    created_by       UUID   │   └───────────────────────────┘
└───────────────────────────┘   │    updated_by       UUID   │   (Module 0 scaffold - no
  (Module 0 - written to by      └───────────────────────────┘    service/controller yet)
   Module 1's auth events)        (Module 0 - written to by
                                   Module 1's auth events)
```

**Why `activity_log` and `notifications` have no foreign keys to `users`:** both are intentionally denormalized. `activity_log.actor_id`/`actor_email` and `notifications.recipient_id` store the user identifier and (for the log) email *as they were at the time of the event*, so the audit trail and notification history remain meaningful even if a user is later deleted — a hard FK with `ON DELETE CASCADE` would silently erase history, and `ON DELETE SET NULL` would lose the actor's identity. This is a deliberate design choice, not an oversight.

## 2. Tables

### `users` (Module 1)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY (app-generated) |
| `first_name` | VARCHAR(100) | NOT NULL |
| `last_name` | VARCHAR(100) | NOT NULL |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `phone` | VARCHAR(20) | nullable |
| `password` | VARCHAR(255) | NOT NULL (BCrypt hash, never plaintext) |
| `role` | VARCHAR(30) | NOT NULL (`ADMIN` today; enum-backed, extensible) |
| `status` | VARCHAR(30) | NOT NULL, DEFAULT `'ACTIVE'` (`ACTIVE`, `INACTIVE`, `SUSPENDED`) |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT `TRUE` |
| `last_login` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL (JPA auditing) |
| `created_by`, `updated_by` | UUID | nullable (JPA auditing — null until a second user exists to act) |

### `refresh_tokens` (Module 1)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `user_id` | UUID | NOT NULL, FK → `users(id)` **ON DELETE CASCADE** |
| `token_hash` | VARCHAR(128) | NOT NULL, UNIQUE (SHA-256 hex digest; raw token is never stored) |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `revoked` | BOOLEAN | NOT NULL, DEFAULT `FALSE` |
| `revoked_at` | TIMESTAMPTZ | nullable |
| `replaced_by_token_hash` | VARCHAR(128) | nullable — links a rotated-out row to the row that replaced it |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

### `password_reset_tokens` (Module 1)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `user_id` | UUID | NOT NULL, FK → `users(id)` **ON DELETE CASCADE** |
| `token_hash` | VARCHAR(128) | NOT NULL, UNIQUE (SHA-256 hex digest) |
| `expires_at` | TIMESTAMPTZ | NOT NULL (1 hour from issuance) |
| `used` | BOOLEAN | NOT NULL, DEFAULT `FALSE` |
| `used_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

### `activity_log` (Module 0, populated by Module 1)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `actor_id` | UUID | nullable (null for unauthenticated/system events, e.g. a failed login for an unknown email) |
| `actor_email` | VARCHAR(255) | nullable |
| `action` | VARCHAR(40) | NOT NULL — enum `ActivityAction`: `CREATE`, `UPDATE`, `DELETE`, `UPLOAD`, `GENERATE`, `APPROVE`, `REJECT`, `PUBLISH`, `SCHEDULE`, `LOGIN`, `LOGIN_FAILED`, `LOGOUT`, `PASSWORD_CHANGE`, `SETTINGS_CHANGE`, `OTHER` |
| `entity_type` | VARCHAR(100) | NOT NULL (e.g. `"User"`) |
| `entity_id` | UUID | nullable |
| `metadata` | TEXT | nullable — JSON-serialized context (e.g. `{"clientIp": "..."}`) |
| `created_at` | TIMESTAMPTZ | NOT NULL |

No `updated_at`/`updated_by`: audit entries are immutable once written, so update-tracking columns do not apply.

### `notifications` (Module 0, populated by Module 1)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `recipient_id` | UUID | nullable (null = broadcast to all admins) |
| `type` | VARCHAR(40) | NOT NULL — enum `NotificationType`: `LOGIN_SUCCESS`, `PASSWORD_CHANGED`, `GENERATION_COMPLETED`, `APPROVAL_REQUIRED`, `APPROVED`, `REJECTED`, `PUBLISH_SUCCESS`, `PUBLISH_FAILURE`, `SCHEDULE_COMPLETED`, `SCHEDULE_FAILED`, `SYSTEM` |
| `title` | VARCHAR(255) | NOT NULL |
| `message` | TEXT | NOT NULL |
| `link` | VARCHAR(500) | nullable (deep link for a future notification-center UI) |
| `is_read` | BOOLEAN | NOT NULL, DEFAULT `FALSE` |
| `read_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

Only `LOGIN_SUCCESS` and `PASSWORD_CHANGED` are actually written today (by Module 1). The other enum values are reserved for later modules.

### `app_settings` (Module 0 scaffold — no service/controller yet)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `setting_key` | VARCHAR(150) | NOT NULL, UNIQUE |
| `setting_value` | TEXT | nullable |
| `category` | VARCHAR(40) | NOT NULL — enum `SettingCategory`: `AI_PROVIDER`, `GENERATION`, `SCHEDULING`, `STORAGE`, `INTEGRATION` |
| `description` | VARCHAR(500) | nullable |
| `is_secret` | BOOLEAN | NOT NULL, DEFAULT `FALSE` |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

This table has no rows written by any current code path — it exists so a future Application Settings module can build its CRUD layer directly on top of an already-migrated schema.

## 3. Relationships

| Relationship | Cardinality | Enforcement |
|---|---|---|
| `users` → `refresh_tokens` | 1 → N | Database FK, `ON DELETE CASCADE` |
| `users` → `password_reset_tokens` | 1 → N | Database FK, `ON DELETE CASCADE` |
| `users` → `activity_log` | logical 1 → N (by `actor_id`) | **Not** a database FK — denormalized by design (see §1) |
| `users` → `notifications` | logical 1 → N (by `recipient_id`) | **Not** a database FK — denormalized by design (see §1) |

`app_settings` has no relationships to any other table.

## 4. Constraints

- `users.email`, `refresh_tokens.token_hash`, `password_reset_tokens.token_hash`, and `app_settings.setting_key` all carry a `UNIQUE` constraint.
- `refresh_tokens.user_id` and `password_reset_tokens.user_id` are `NOT NULL` foreign keys with `ON DELETE CASCADE` — deleting a user immediately invalidates all of their tokens.
- All boolean flags (`is_active`, `revoked`, `used`, `is_read`, `is_secret`) are `NOT NULL` with an explicit `DEFAULT`.
- All timestamp columns use `TIMESTAMPTZ` (timezone-aware), and Hibernate is configured with `hibernate.jdbc.time_zone: UTC` so every stored instant is unambiguous.

## 5. Indexes

| Index | Table | Columns | Purpose |
|---|---|---|---|
| `idx_users_email` | `users` | `email` | Login lookup (`findByEmailIgnoreCase`) |
| `idx_refresh_tokens_user` | `refresh_tokens` | `user_id, revoked` | "all active tokens for user" queries (logout-all, password change) |
| `idx_refresh_tokens_hash` | `refresh_tokens` | `token_hash` | Refresh/rotate lookup by presented token |
| `idx_password_reset_tokens_user` | `password_reset_tokens` | `user_id` | Future lookups by user |
| `idx_activity_log_entity` | `activity_log` | `entity_type, entity_id` | "history for this record" queries |
| `idx_activity_log_actor` | `activity_log` | `actor_id` | "history by this user" queries |
| `idx_activity_log_action` | `activity_log` | `action` | Filtering by action type |
| `idx_activity_log_created_at` | `activity_log` | `created_at DESC` | Recent-first listing |
| `idx_notifications_recipient_unread` | `notifications` | `recipient_id, is_read` | Unread-count / inbox queries |
| `idx_notifications_created_at` | `notifications` | `created_at DESC` | Recent-first listing |
| `idx_app_settings_category` | `app_settings` | `category` | Grouped settings retrieval |

Every index above backs an actual repository query method that exists in the codebase today (e.g. `UserRepository.findByEmailIgnoreCase`, `RefreshTokenRepository.findByUserIdAndRevokedFalse`) — none are speculative.

## 6. Flyway Migrations

| Version | File | Module | Contents |
|---|---|---|---|
| `V1` | `V1__init_schema.sql` | Module 0 | `activity_log`, `notifications`, `app_settings` + their indexes |
| `V2` | `V2__auth_and_users.sql` | Module 1 | `users`, `refresh_tokens`, `password_reset_tokens` + their indexes and foreign keys |

Both have been applied successfully against a real PostgreSQL 16 instance (verified via `docker compose up` and via the Testcontainers-backed `AuthControllerIntegrationTest`, which boots a disposable Postgres container and runs both migrations before any test executes).

## 7. Future Database Roadmap

Not yet designed or migrated — listed here only to show what the current schema deliberately leaves room for, not as a commitment to a specific column layout:

- **Media** table(s) for the Media Upload module (file metadata, MinIO object keys, upload type).
- **Content draft** table(s) for AI-generated content (versioned, linked to media).
- **Approval** state/audit table(s) for the approval workflow (may reuse `activity_log` rather than a new table).
- **Publish record** table(s) for Instagram/website publishing history.
- **Schedule** table(s) for the scheduler module.
- Full CRUD usage of the existing `app_settings` table by the Application Settings module.
- Possible introduction of additional `Role` enum values (e.g. `EDITOR`) — the `users.role` column already supports this without a migration, since it is a plain `VARCHAR` validated at the application layer, not a database `CHECK`/`ENUM` type.
