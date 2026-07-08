# Database Design — Arjun Sports AI Content Agent

**Engine:** PostgreSQL 16. **Schema ownership:** Flyway migrations only (`spring.jpa.hibernate.ddl-auto: validate` — Hibernate never generates or alters DDL, it only verifies entity mappings match what Flyway created). **Primary keys:** every table uses a `UUID` generated application-side by Hibernate (`@UuidGenerator`), so no table relies on a database-side default or the `pgcrypto`/`uuid-ossp` extensions.

This document covers exactly the fourteen tables that exist after migrations `V1` through `V7`. No table described here is speculative.

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

### Modules 2–4 additions (`V3`–`V6`)

```
┌───────────────────────────┐
│          media              │
├───────────────────────────┤
│ PK id               UUID   │
│    file_name        VARCHAR│
│    storage_key UNIQUE VARCHAR│
│    content_type     VARCHAR│
│    media_type       VARCHAR│
│    file_size_bytes   BIGINT│
│    description      VARCHAR│
│    is_deleted       BOOLEAN│
│    created_at   TIMESTAMPTZ│
│    updated_at   TIMESTAMPTZ│
│    created_by       UUID   │
│    updated_by       UUID   │
└─────────────┬─────────────┘
              │ 0..1 (ON DELETE SET NULL, plain UUID column - no @ManyToOne)
              ▼
┌───────────────────────────┐        ┌───────────────────────────┐
│    prompt_templates         │        │      generated_content       │
├───────────────────────────┤        ├───────────────────────────┤
│ PK id               UUID   │        │ PK id               UUID   │
│    content_type     VARCHAR│◄───────┤ FK media_id ────────UUID   │
│    name             VARCHAR│  0..1   │ FK content_type     VARCHAR│
│    description      VARCHAR│ (by     │ FK prompt_template_id UUID │──┐
│    system_prompt        TEXT│  value, │    prompt_used          TEXT│  │ ON DELETE
│    user_prompt_template  TEXT│  not FK)│    generated_text       TEXT│  │ SET NULL
│    version          INTEGER│         │    ai_model          VARCHAR│  │
│    is_active        BOOLEAN│         │    status            VARCHAR│  │
│    created_at   TIMESTAMPTZ│         │    error_message         TEXT│  │
│    updated_at   TIMESTAMPTZ│         │    is_draft          BOOLEAN│  │
│    created_by       UUID   │         │    is_edited         BOOLEAN│  │
│    updated_by       UUID   │         │    created_at   TIMESTAMPTZ│  │
└─────────────┬─────────────┘         │    updated_at   TIMESTAMPTZ│  │
   0..N (unique partial index:        │    created_by       UUID   │  │
   one is_active=TRUE row per         │    updated_by       UUID   │  │
   content_type)                      └──────────┬──────────────┘  │
                                                   │ 0..N              │
                          ┌────────────────────────┘ (ON DELETE        │
                          ▼                          SET NULL)         │
              ┌───────────────────────────┐                            │
              │     generation_history       │◄───────────────────────┘
              ├───────────────────────────┤   (prompt_template_id, no FK constraint -
              │ PK id               UUID   │    plain denormalized column)
              │ FK generated_content_id UUID│
              │    media_id          UUID   │  (immutable append-only log - no
              │    content_type      VARCHAR│   updated_at/updated_by; a FAILED row
              │    prompt_template_id UUID  │   must outlive the transaction that
              │    action            VARCHAR│   created it, see ARCHITECTURE.md §6)
              │    ai_model          VARCHAR│
              │    status            VARCHAR│
              │    error_message         TEXT│
              │    latency_ms        INTEGER│
              │    actor_id          UUID   │
              │    actor_email       VARCHAR│
              │    created_at   TIMESTAMPTZ│
              └───────────────────────────┘

┌───────────────────────────┐
│      content_drafts          │
├───────────────────────────┤
│ PK id               UUID   │
│ FK generated_content_id UUID│──── ON DELETE SET NULL → generated_content(id)
│ FK media_id          UUID   │──── ON DELETE SET NULL → media(id)
│    content_type     VARCHAR│
│    title            VARCHAR│
│    content_text          TEXT│  (own copy - independently editable from the
│    status            VARCHAR│   source generated_content's text from this
│    is_deleted        BOOLEAN│   point on; see ARCHITECTURE.md §15)
│    deleted_at   TIMESTAMPTZ│
│    created_at   TIMESTAMPTZ│
│    updated_at   TIMESTAMPTZ│
│    created_by       UUID   │
│    updated_by       UUID   │
└─────────────┬─────────────┘
              │ 0..N (ON DELETE SET NULL, plain UUID column - no @ManyToOne)
              ▼
┌───────────────────────────┐
│         approvals            │
├───────────────────────────┤
│ PK id               UUID   │
│ FK content_id        UUID   │──── ON DELETE SET NULL → content_drafts(id)
│    content_title    VARCHAR│  (denormalized snapshot for search/display,
│    content_type     VARCHAR│   same reasoning as content_drafts above)
│    reviewer_id       UUID   │
│    status            VARCHAR│
│    remarks               TEXT│
│    approved_at   TIMESTAMPTZ│
│    rejected_at   TIMESTAMPTZ│
│    created_at   TIMESTAMPTZ│
│    updated_at   TIMESTAMPTZ│
│    created_by       UUID   │  (the submitter, via JPA auditing - no
│    updated_by       UUID   │   separate submitted_by column needed)
└──────┬────────────────┬───┘
   0..N│(ON DELETE SET NULL)  │0..N (ON DELETE CASCADE)
       ▼                      ▼
┌───────────────────┐  ┌───────────────────┐
│  approval_history    │  │  approval_comments   │
├───────────────────┤  ├───────────────────┤
│ PK id         UUID   │  │ PK id         UUID   │
│    approval_id UUID  │  │    approval_id UUID  │
│    content_id  UUID  │  │    content_id  UUID  │
│    action      VARCHAR│  │    author_id   UUID  │
│    previous_status   │  │    author_email VARCHAR│
│         VARCHAR      │  │    comment         TEXT│
│    new_status VARCHAR│  │    created_at TIMESTAMPTZ│
│    actor_id    UUID  │  └───────────────────┘
│    actor_email VARCHAR│  (comments are owned by their approval -
│    remarks        TEXT│   CASCADE, unlike history which survives
│    created_at TIMESTAMPTZ│  independently)
└───────────────────┘
(immutable append-only log, like generation_history -
 no updated_at/updated_by)
```

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
| `type` | VARCHAR(40) | NOT NULL — enum `NotificationType`: `LOGIN_SUCCESS`, `PASSWORD_CHANGED`, `GENERATION_COMPLETED`, `GENERATION_FAILED`, `DRAFT_SAVED`, `DRAFT_UPDATED`, `DRAFT_DELETED`, `DRAFT_RESTORED`, `APPROVAL_REQUIRED`, `APPROVED`, `REJECTED`, `PUBLISH_SUCCESS`, `PUBLISH_FAILURE`, `SCHEDULE_COMPLETED`, `SCHEDULE_FAILED`, `SYSTEM` |
| `title` | VARCHAR(255) | NOT NULL |
| `message` | TEXT | NOT NULL |
| `link` | VARCHAR(500) | nullable (deep link for a future notification-center UI) |
| `is_read` | BOOLEAN | NOT NULL, DEFAULT `FALSE` |
| `read_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

`LOGIN_SUCCESS`/`PASSWORD_CHANGED` (Module 1), `GENERATION_COMPLETED`/`GENERATION_FAILED` (Module 3), and `DRAFT_SAVED`/`DRAFT_UPDATED`/`DRAFT_DELETED`/`DRAFT_RESTORED` (Module 4) are written today. `APPROVAL_REQUIRED` onward are reserved for later modules.

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

### `media` (Module 2)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `file_name` | VARCHAR(255) | NOT NULL |
| `storage_key` | VARCHAR(500) | NOT NULL, UNIQUE (the MinIO object key) |
| `content_type` | VARCHAR(100) | NOT NULL (validated against a per-`media_type` allow-list at the application layer) |
| `media_type` | VARCHAR(20) | NOT NULL — enum `MediaType`: `IMAGE`, `VIDEO`, `PDF`, `TEXT_NOTE` |
| `file_size_bytes` | BIGINT | NOT NULL (validated against a per-`media_type` max at the application layer: 10MB/200MB/20MB/2MB) |
| `description` | VARCHAR(1000) | nullable |
| `is_deleted` | BOOLEAN | NOT NULL, DEFAULT `FALSE` (soft delete — the MinIO object is not removed) |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

### `prompt_templates` (Module 3)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `content_type` | VARCHAR(40) | NOT NULL — enum `ContentType`, one of the 17 supported content types |
| `name` | VARCHAR(200) | NOT NULL |
| `description` | VARCHAR(500) | nullable |
| `system_prompt` | TEXT | NOT NULL — supports `{{academyName}}` |
| `user_prompt_template` | TEXT | NOT NULL — supports `{{academyName}}`, `{{mediaFileName}}`, `{{mediaDescription}}`, `{{manualNotes}}`, `{{eventDetails}}`, `{{achievement}}`, `{{competitionResults}}`, `{{trainingSession}}`, `{{coachNotes}}` |
| `version` | INTEGER | NOT NULL |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT `TRUE` |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

Rows are immutable once written — "editing" a template via `PUT /api/prompts/{id}` deactivates the current row and inserts a new one at `version + 1`, so every past version stays queryable by `content_type` history.

### `generated_content` (Module 3)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `media_id` | UUID | nullable, FK → `media(id)` **ON DELETE SET NULL** |
| `content_type` | VARCHAR(40) | NOT NULL |
| `prompt_template_id` | UUID | nullable, FK → `prompt_templates(id)` **ON DELETE SET NULL** |
| `prompt_used` | TEXT | NOT NULL (the fully-resolved user prompt actually sent to the AI provider) |
| `generated_text` | TEXT | nullable (null when `status = FAILED`) |
| `ai_model` | VARCHAR(100) | nullable |
| `status` | VARCHAR(20) | NOT NULL — enum `GenerationStatus`: `SUCCESS`, `FAILED` |
| `error_message` | TEXT | nullable |
| `is_draft` | BOOLEAN | NOT NULL, DEFAULT `TRUE` (Module 3's own lightweight draft/final toggle — distinct from Module 4's `content_drafts` workflow, see `ARCHITECTURE.md` §15) |
| `is_edited` | BOOLEAN | NOT NULL, DEFAULT `FALSE` |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

### `generation_history` (Module 3)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `generated_content_id` | UUID | nullable, FK → `generated_content(id)` **ON DELETE SET NULL** |
| `media_id` | UUID | nullable (denormalized, no FK) |
| `content_type` | VARCHAR(40) | NOT NULL (denormalized, no FK) |
| `prompt_template_id` | UUID | nullable (denormalized, no FK) |
| `action` | VARCHAR(20) | NOT NULL — enum `GenerationAction`: `GENERATE`, `REGENERATE` |
| `ai_model` | VARCHAR(100) | nullable |
| `status` | VARCHAR(20) | NOT NULL |
| `error_message` | TEXT | nullable |
| `latency_ms` | INTEGER | nullable |
| `actor_id` | UUID | nullable (denormalized, no FK — same reasoning as `activity_log`) |
| `actor_email` | VARCHAR(255) | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL |

No `updated_at`/`updated_by`: like `activity_log`, this is an immutable append-only log — every generate/regenerate attempt (success or failure) gets its own row, never updated in place. A `FAILED` row is written via a `REQUIRES_NEW` transaction specifically so it survives even when the triggering request's own transaction rolls back (see `ARCHITECTURE.md` §6, `GenerationFailureRecorder`).

### `content_drafts` (Module 4)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `generated_content_id` | UUID | nullable, FK → `generated_content(id)` **ON DELETE SET NULL** |
| `media_id` | UUID | nullable, FK → `media(id)` **ON DELETE SET NULL** |
| `content_type` | VARCHAR(40) | NOT NULL (denormalized from the source `generated_content` at creation time) |
| `title` | VARCHAR(200) | NOT NULL (defaults to `"<Content Type> Draft"` if not supplied at creation) |
| `content_text` | TEXT | NOT NULL (copied from the source's `generated_text` at creation, then independently editable) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `'DRAFT'` — enum `DraftStatus`: `DRAFT`, `READY_FOR_REVIEW`, `APPROVED`, `REJECTED`, `PUBLISHED`, `ARCHIVED` |
| `is_deleted` | BOOLEAN | NOT NULL, DEFAULT `FALSE` (soft delete — restorable via `POST /api/drafts/{id}/restore`) |
| `deleted_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns |

Business rule enforced at the service layer (not a DB constraint, for a clearer error message than a raw constraint violation): only one non-deleted draft per `generated_content_id` may be `APPROVED` at a time (`DraftRepository.existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot`).

### `approvals` (Module 5)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `content_id` | UUID | nullable, FK → `content_drafts(id)` **ON DELETE SET NULL** |
| `content_title` | VARCHAR(200) | nullable (denormalized from the draft at submission time, for search/display) |
| `content_type` | VARCHAR(40) | nullable (denormalized from the draft at submission time) |
| `reviewer_id` | UUID | nullable (set only once approved or rejected — null while `PENDING_APPROVAL`) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `'PENDING_APPROVAL'` — enum `ApprovalStatus`: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `READY_FOR_PUBLISH` |
| `remarks` | TEXT | nullable (the latest reviewer note — set on approve/reject) |
| `approved_at` | TIMESTAMPTZ | nullable |
| `rejected_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at`, `created_by`, `updated_by` | — | JPA auditing columns (`created_by` doubles as "who submitted this" — no separate column needed) |

Business rule enforced at the database level: `idx_approvals_one_pending_per_content` is a **partial unique index** — `UNIQUE (content_id) WHERE status = 'PENDING_APPROVAL'` — so at most one active review request can exist per draft at a time, the same defense-in-depth pattern as `prompt_templates`' one-active-version-per-type index. A draft rejected and resubmitted later accumulates a new `approvals` row rather than reusing the old one, so its full review history (across every submission attempt) is preserved.

### `approval_history` (Module 5)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `approval_id` | UUID | nullable, FK → `approvals(id)` **ON DELETE SET NULL** |
| `content_id` | UUID | nullable (denormalized, no FK) |
| `action` | VARCHAR(20) | NOT NULL — enum `ApprovalAction`: `SUBMIT`, `APPROVE`, `REJECT`, `COMMENT` |
| `previous_status` | VARCHAR(20) | nullable (null for the initial `SUBMIT` entry) |
| `new_status` | VARCHAR(20) | NOT NULL |
| `actor_id` | UUID | nullable (denormalized, no FK) |
| `actor_email` | VARCHAR(255) | nullable |
| `remarks` | TEXT | nullable (the comment text, for `COMMENT` entries) |
| `created_at` | TIMESTAMPTZ | NOT NULL |

No `updated_at`/`updated_by`: like `generation_history`, this is an immutable append-only log — every submit/approve/reject/comment action gets its own row, never updated in place.

### `approval_comments` (Module 5)

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `approval_id` | UUID | NOT NULL, FK → `approvals(id)` **ON DELETE CASCADE** |
| `content_id` | UUID | nullable (denormalized, no FK) |
| `author_id` | UUID | nullable |
| `author_email` | VARCHAR(255) | nullable |
| `comment` | TEXT | NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL |

Unlike every other Module 2–5 foreign key (`ON DELETE SET NULL`), this one is **`ON DELETE CASCADE`**: a comment has no independent meaning once its parent approval is gone, whereas `approval_history` is a cross-cutting audit record expected to outlive whatever it references (see §1 for the same reasoning applied to `activity_log`/`notifications`).

## 3. Relationships

| Relationship | Cardinality | Enforcement |
|---|---|---|
| `users` → `refresh_tokens` | 1 → N | Database FK, `ON DELETE CASCADE` |
| `users` → `password_reset_tokens` | 1 → N | Database FK, `ON DELETE CASCADE` |
| `users` → `activity_log` | logical 1 → N (by `actor_id`) | **Not** a database FK — denormalized by design (see §1) |
| `users` → `notifications` | logical 1 → N (by `recipient_id`) | **Not** a database FK — denormalized by design (see §1) |
| `media` → `generated_content` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `media` → `content_drafts` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `prompt_templates` → `generated_content` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `generated_content` → `generation_history` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `generated_content` → `content_drafts` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `content_drafts` → `approvals` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `approvals` → `approval_history` | 0..1 → N | Database FK, `ON DELETE SET NULL` |
| `approvals` → `approval_comments` | 1 → N | Database FK, `ON DELETE CASCADE` |

`app_settings` has no relationships to any other table. Every FK introduced in Modules 2–5 uses `ON DELETE SET NULL` rather than `CASCADE` or a hard `@ManyToOne` — deleting a `media`/`generated_content`/`prompt_templates`/`content_drafts` row never cascades or fails; dependent rows just lose the back-reference and keep whatever they've denormalized for their own display (see `ARCHITECTURE.md` §15) — **except** `approval_comments`, which is intentionally `CASCADE` since a comment has no meaning independent of its parent approval.

## 4. Constraints

- `users.email`, `refresh_tokens.token_hash`, `password_reset_tokens.token_hash`, `app_settings.setting_key`, and `media.storage_key` all carry a `UNIQUE` constraint.
- `refresh_tokens.user_id` and `password_reset_tokens.user_id` are `NOT NULL` foreign keys with `ON DELETE CASCADE` — deleting a user immediately invalidates all of their tokens.
- `prompt_templates` has a **partial unique index** — `UNIQUE (content_type) WHERE is_active = TRUE` — enforcing exactly one active template per content type at the database level (not just the application layer).
- `approvals` has a **partial unique index** — `UNIQUE (content_id) WHERE status = 'PENDING_APPROVAL'` — enforcing at most one outstanding review request per draft at the database level, the same defense-in-depth pattern as `prompt_templates`.
- `approval_comments.approval_id` is a `NOT NULL` foreign key with `ON DELETE CASCADE` — deleting an approval removes its comment thread.
- All boolean flags (`is_active`, `revoked`, `used`, `is_read`, `is_secret`, `is_deleted`, `is_draft`, `is_edited`) are `NOT NULL` with an explicit `DEFAULT`.
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
| `idx_media_deleted_type` | `media` | `is_deleted, media_type` | Media library listing filtered by type |
| `idx_media_created_at` | `media` | `created_at DESC` | Recent-first listing |
| `idx_prompt_templates_active_per_type` | `prompt_templates` | `content_type` (unique, partial `WHERE is_active`) | Resolving the active template at generation time; enforces the one-active-per-type rule |
| `idx_prompt_templates_content_type` | `prompt_templates` | `content_type` | Version history lookup |
| `idx_generated_content_media` | `generated_content` | `media_id` | "content generated from this media" queries |
| `idx_generated_content_type` | `generated_content` | `content_type` | Filtering the content library by type |
| `idx_generated_content_created_at` | `generated_content` | `created_at DESC` | Recent-first listing |
| `idx_generation_history_content` | `generation_history` | `generated_content_id` | History for one piece of content |
| `idx_generation_history_created_at` | `generation_history` | `created_at DESC` | Recent-first listing |
| `idx_content_drafts_generated_content` | `content_drafts` | `generated_content_id` | Duplicate-final-draft check, source lookup |
| `idx_content_drafts_status` | `content_drafts` | `status` | Status filter on `GET /api/drafts` |
| `idx_content_drafts_content_type` | `content_drafts` | `content_type` | Content-type filter on `GET /api/drafts` |
| `idx_content_drafts_created_at` | `content_drafts` | `created_at DESC` | Recent-first listing |
| `idx_content_drafts_deleted` | `content_drafts` | `is_deleted` | Default "exclude trash" filter |
| `idx_approvals_content` | `approvals` | `content_id` | Duplicate-pending check, history/detail lookups |
| `idx_approvals_status` | `approvals` | `status` | Status filter on `GET /api/approval/pending` |
| `idx_approvals_created_at` | `approvals` | `created_at DESC` | Recent-first listing, date-range filter |
| `idx_approvals_one_pending_per_content` | `approvals` | `content_id` (unique, partial `WHERE status = 'PENDING_APPROVAL'`) | Enforces at most one active review request per draft |
| `idx_approval_history_content` | `approval_history` | `content_id` | `GET /api/approval/history/{contentId}` |
| `idx_approval_history_approval` | `approval_history` | `approval_id` | History for one specific review cycle |
| `idx_approval_history_created_at` | `approval_history` | `created_at DESC` | Recent-first listing |
| `idx_approval_comments_approval` | `approval_comments` | `approval_id, created_at` | Comment thread for one approval, oldest-first |

Every index above backs an actual repository query method or `Specification` predicate that exists in the codebase today (e.g. `UserRepository.findByEmailIgnoreCase`, `RefreshTokenRepository.findByUserIdAndRevokedFalse`, `DraftSpecifications`, `ApprovalSpecifications`) — none are speculative.

## 6. Flyway Migrations

| Version | File | Module | Contents |
|---|---|---|---|
| `V1` | `V1__init_schema.sql` | Module 0 | `activity_log`, `notifications`, `app_settings` + their indexes |
| `V2` | `V2__auth_and_users.sql` | Module 1 | `users`, `refresh_tokens`, `password_reset_tokens` + their indexes and foreign keys |
| `V3` | `V3__media.sql` | Module 2 | `media` + its indexes |
| `V4` | `V4__ai_content_generation.sql` | Module 3 | `prompt_templates`, `generated_content`, `generation_history` + their indexes and foreign keys |
| `V5` | `V5__prompt_template_seed.sql` | Module 3 | Seeds one active `prompt_templates` row per content type (17 rows) |
| `V6` | `V6__content_drafts.sql` | Module 4 | `content_drafts` + its indexes and foreign keys |
| `V7` | `V7__approval_workflow.sql` | Module 5 | `approvals`, `approval_history`, `approval_comments` + their indexes and foreign keys |

All seven have been applied successfully against a real PostgreSQL 16 instance (verified via `docker compose up` and via the Testcontainers-backed integration tests for each module, each of which boots a disposable Postgres container and runs every migration before its tests execute).

## 7. Future Database Roadmap

Not yet designed or migrated — listed here only to show what the current schema deliberately leaves room for, not as a commitment to a specific column layout:

- **Publish record** table(s) for Instagram/website publishing history, most likely referencing `approvals(id)` or `content_drafts(id)` the same `ON DELETE SET NULL` way Module 5 references `content_drafts`.
- **Schedule** table(s) for the scheduler module.
- Full CRUD usage of the existing `app_settings` table by the Application Settings module.
- Possible introduction of additional `Role` enum values (e.g. `EDITOR`, or a distinct reviewer/approver role) — the `users.role` column already supports this without a migration, since it is a plain `VARCHAR` validated at the application layer, not a database `CHECK`/`ENUM` type. Module 5's "Only ADMIN can approve" rule is enforced purely at `@PreAuthorize`, so introducing a narrower reviewer role later is a controller-annotation change, not a schema change.
