# API Documentation — Arjun Sports AI Content Agent

**Base URL (local):** `http://localhost:8080/api`
**Scope:** only endpoints that exist in the codebase today (Module 0 health check, Module 1 auth/user, Module 2 media, Module 3 AI generation/prompts, Module 4 content drafts, Module 5 approval workflow, Module 6 Instagram publisher). Interactive documentation is also available at `/swagger-ui.html` and `/api-docs` when the `dev` Spring profile is active (both are disabled in the `prod` profile).

## Response Envelope

Every endpoint — success or failure — returns the same JSON shape (`ApiResponse<T>`). Fields with a `null` value are omitted from the JSON (`@JsonInclude(NON_NULL)`), so a success response typically has no `errors` key and an error response typically has no `data` key.

```json
{
  "success": true,
  "message": "Optional human-readable message",
  "data": { "...": "endpoint-specific payload" },
  "errors": ["Optional list of field-level validation errors"],
  "timestamp": "2026-07-07T06:29:25.682Z"
}
```

List endpoints wrap a `PageResponse<T>` inside `data`:

```json
{
  "content": [ { "...": "item" } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

## Authentication Requirements

- **Public** endpoints require no credentials.
- **Authenticated** endpoints require a valid `access_token` cookie (set automatically by `/api/auth/login` or `/api/auth/refresh`), or an `Authorization: Bearer <token>` header as a fallback for non-browser clients.
- **ADMIN** endpoints additionally require the authenticated user's `role` to be `ADMIN` (the only role that exists today).
- All cookies are `HttpOnly`; the refresh cookie is further scoped to the `/api/auth` path only, so it is never sent to non-auth endpoints.

## Status Codes Used

| Code | Meaning | Where it comes from |
|---|---|---|
| 200 | Success | Normal responses |
| 201 | Created | `POST /api/users` |
| 400 | Bad request / validation failure | `GlobalExceptionHandler` (Bean Validation, `BadRequestException`) |
| 401 | Unauthorized | Missing/invalid/expired token, wrong credentials, `UnauthorizedException`, Spring Security's `RestAuthenticationEntryPoint`, or `InstagramPublisherException` — `INVALID_CREDENTIALS` (Meta rejected the access token) |
| 403 | Forbidden | Authenticated but wrong role — `RestAccessDeniedHandler` |
| 404 | Not found | `ResourceNotFoundException` |
| 409 | Conflict | `ConflictException` (e.g. duplicate email, duplicate final draft, already-connected Instagram account, already-published content) |
| 413 | Payload too large | Uploaded file exceeds the type's max size |
| 422 | Unprocessable entity | `InstagramPublisherException` — `INVALID_MEDIA` (Meta rejected the image itself) |
| 429 | Too many requests | Login or AI generation rate limiter exceeded, or `InstagramPublisherException` — `RATE_LIMITED` |
| 500 | Internal server error | Any uncaught exception (`GlobalExceptionHandler` fallback) |
| 502/503/504 | AI provider / Instagram publisher error | `AIProviderException` — `INVALID_RESPONSE`/`UNKNOWN` → 502, `UNAVAILABLE` → 503, `TIMEOUT` → 504; `InstagramPublisherException` — `UNAVAILABLE`/`UNKNOWN` → 503, `TIMEOUT` → 504 |

## Error Response Shape

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    "email: Email must be a valid address",
    "password: Password is required"
  ],
  "timestamp": "2026-07-07T06:29:25.682Z"
}
```

A non-validation error omits `errors` entirely:

```json
{
  "success": false,
  "message": "Invalid email or password",
  "timestamp": "2026-07-07T06:29:25.682Z"
}
```

---

## Health API

### `GET /api/health`

Public. Liveness check.

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "content-agent-backend",
    "timestamp": "2026-07-07T06:29:25.682Z"
  },
  "timestamp": "2026-07-07T06:29:25.682Z"
}
```

---

## Authentication APIs

### `POST /api/auth/login`

Public. Authenticates a user and sets the `access_token` and `refresh_token` HttpOnly cookies on success.

**Request:**
```json
{
  "email": "admin@arjunsports.example",
  "password": "DevAdmin123"
}
```

Validation: `email` required + valid email format; `password` required.

**Response `200`** (also sets `Set-Cookie: access_token=...` and `Set-Cookie: refresh_token=...`):
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": "3f7b1e2a-...",
    "firstName": "Academy",
    "lastName": "Admin",
    "email": "admin@arjunsports.example",
    "phone": null,
    "role": "ADMIN",
    "status": "ACTIVE",
    "active": true,
    "lastLogin": "2026-07-07T06:29:25.682Z",
    "createdAt": "2026-07-01T10:00:00Z",
    "updatedAt": "2026-07-07T06:29:25.682Z"
  },
  "timestamp": "2026-07-07T06:29:25.682Z"
}
```

**Errors:**
- `401` — wrong email or password (identical message for both, to avoid revealing which one was wrong):
  ```json
  { "success": false, "message": "Invalid email or password", "timestamp": "..." }
  ```
- `429` — more than 5 attempts from the same client IP within a minute:
  ```json
  { "success": false, "message": "Too many login attempts. Please try again in a minute.", "timestamp": "..." }
  ```

### `POST /api/auth/refresh`

Public (but requires a valid `refresh_token` cookie — there is no request body). Rotates the refresh token and issues a new access token.

**Response `200`:** same shape as login, with fresh `Set-Cookie` headers for both tokens.

**Errors:**
- `401` — no refresh token cookie present, token unknown/expired, or the token had already been rotated out (reuse detection — in this case every session for the user is also revoked server-side).

### `POST /api/auth/logout`

**Authenticated.** Revokes the refresh token presented in the request and clears both cookies.

**Response `200`:**
```json
{ "success": true, "message": "Logged out", "timestamp": "..." }
```

### `GET /api/auth/me`

**Authenticated.** Returns the profile of the currently authenticated user.

**Response `200`:** same user object shape as the login response's `data`.

**Errors:** `401` if the access token is missing, invalid, or expired.

### `POST /api/auth/change-password`

**Authenticated.** Changes the current user's password and revokes every other active session (a fresh token pair is issued for the current session, so the caller stays logged in).

**Request:**
```json
{
  "currentPassword": "DevAdmin123",
  "newPassword": "NewPassw0rd123"
}
```

Validation: `currentPassword` required; `newPassword` required, must match the password-strength pattern (minimum 8 characters, at least one letter and one digit).

**Response `200`:** same user object shape, plus fresh `Set-Cookie` headers.

**Errors:**
- `400` — `currentPassword` does not match the stored password:
  ```json
  { "success": false, "message": "Current password is incorrect", "timestamp": "..." }
  ```
- `400` — `newPassword` fails the strength pattern (validation error list as shown above).

### `POST /api/auth/forgot-password`

Public. Always returns the same response whether or not the email exists, to prevent account enumeration. If the email does exist, a password reset token is generated and persisted (hashed) with a 1-hour expiry.

**Request:**
```json
{ "email": "admin@arjunsports.example" }
```

**Response `200`** (always, regardless of whether the account exists):
```json
{
  "success": true,
  "message": "If an account exists for this email, password reset instructions will be sent",
  "timestamp": "..."
}
```

> **Note:** email delivery is not implemented yet. The token is generated and stored but never emailed to the user in the current implementation — see `IMPLEMENTATION_REPORT.md` §12.

### `POST /api/auth/reset-password`

Public. Redeems a password reset token issued by `forgot-password`.

**Request:**
```json
{
  "token": "<raw token value>",
  "newPassword": "NewPassw0rd123"
}
```

**Response `200`:**
```json
{ "success": true, "message": "Password reset successfully", "timestamp": "..." }
```

**Errors:** `400` if the token is unknown, already used, or expired:
```json
{ "success": false, "message": "Invalid or expired reset token", "timestamp": "..." }
```

---

## User Management APIs (ADMIN only)

All endpoints under `/api/users/**` require the authenticated user's role to be `ADMIN`. A non-admin authenticated user gets `403`; an unauthenticated caller gets `401`.

### `POST /api/users`

Creates a new user.

**Request:**
```json
{
  "firstName": "Jane",
  "lastName": "Coach",
  "email": "jane.coach@arjunsports.example",
  "phone": "+919876543210",
  "password": "CoachPass123",
  "role": "ADMIN"
}
```

Validation: `firstName`/`lastName` required (max 100 chars); `email` required, valid format; `phone` optional but must match `^\+?[1-9]\d{7,14}$` if present; `password` required, must match the password-strength pattern; `role` required (`ADMIN` is the only value that exists today).

**Response `201`:**
```json
{
  "success": true,
  "message": "User created",
  "data": {
    "id": "9c2e...",
    "firstName": "Jane",
    "lastName": "Coach",
    "email": "jane.coach@arjunsports.example",
    "phone": "+919876543210",
    "role": "ADMIN",
    "status": "ACTIVE",
    "active": true,
    "lastLogin": null,
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

**Errors:** `409` if the email is already registered.

### `GET /api/users`

Paginated user list. Accepts standard Spring Data `Pageable` query parameters: `page`, `size`, `sort` (e.g. `GET /api/users?page=0&size=20&sort=lastName,asc`).

**Response `200`:** `data` is a `PageResponse<UserResponse>` (see the envelope section above).

### `GET /api/users/{id}`

**Response `200`:** a single `UserResponse`. **Errors:** `404` if the id does not exist.

### `PUT /api/users/{id}`

Updates a user's profile, role, status, and active flag (email and password are not editable through this endpoint).

**Request:**
```json
{
  "firstName": "Jane",
  "lastName": "Coach",
  "phone": "+919876543210",
  "role": "ADMIN",
  "status": "ACTIVE",
  "active": true
}
```

**Response `200`:** the updated `UserResponse`. **Errors:** `404` if the id does not exist; `400` on validation failure.

---

## Media APIs (Authenticated)

### `POST /api/media/upload`

Multipart upload. Fields: `file` (required), `description` (optional). Per-type limits: IMAGE 10MB, VIDEO 200MB, PDF 20MB, TEXT_NOTE 2MB; content-type is validated against an allow-list per type.

**Response `200`:** the created `MediaResponse` (`id`, `fileName`, `mediaType`, `contentType`, `fileSizeBytes`, `description`, `url` — a presigned GET URL, `createdAt`). **Errors:** `400` invalid/oversized file; `413` if the file exceeds Spring's global max upload size.

### `GET /api/media`

Paginated, non-deleted media list. Query params: `page`, `size`, `sort`, optional `mediaType`.

### `DELETE /api/media/{id}`

Soft-deletes the media row (the object itself is not removed from storage). **Errors:** `404` if not found or already deleted.

---

## AI Content Generation APIs (ADMIN only)

### `POST /api/ai/generate`

**Request:**
```json
{
  "mediaId": null,
  "contentType": "INSTAGRAM_CAPTION",
  "manualNotes": "Team won gold at the regional championship",
  "eventDetails": null,
  "achievement": "1st place, Regional Championship",
  "competitionResults": null,
  "trainingSession": null,
  "coachNotes": null
}
```
At least one context field (or `mediaId` with a description) must be non-blank. `contentType` must be one of the 17 supported types.

**Response `200`:** the created `GeneratedContentResponse` (`id`, `mediaId`, `contentType`, `promptTemplateId`, `generatedText`, `aiModel`, `status`, `errorMessage`, `draft`, `edited`, timestamps). **Errors:** `400` no context provided or no active template for the type; `429` rate limit (10/min/user); `502`/`503`/`504` provider error (a `FAILED` row is still persisted to `generation_history`).

### `POST /api/ai/regenerate`

**Request:** `{ "contentId": "<uuid>" }`. Re-runs the same resolved prompt through the same template. **Response `200`:** the updated `GeneratedContentResponse`. **Errors:** `404` unknown id; `400` content has no associated prompt template.

### `GET /api/ai/history`

Paginated `GenerationHistoryResponse` list (every generate/regenerate attempt, success or failure), newest first.

### `GET /api/ai/content` / `GET /api/ai/content/{id}`

Paginated list (optional `mediaId`, `contentType` filters) or single-item lookup.

### `PUT /api/ai/content/{id}`

**Request:** `{ "generatedText": "...", "draft": true }`. Marks `edited: true` if the text changed. **Response `200`:** the updated item.

### `DELETE /api/ai/content/{id}`

Hard-deletes the generated content row.

---

## Prompt Template APIs (ADMIN only)

### `GET /api/prompts`

Lists all active prompt templates (one per content type).

### `PUT /api/prompts/{id}`

**Request:** `{ "systemPrompt": "...", "userPromptTemplate": "..." }`. Deactivates the current version and inserts a new one at `version + 1` for the same content type — templates are never mutated in place, so history is preserved. **Response `200`:** the newly-active `PromptTemplateResponse`.

---

## Content Draft APIs (ADMIN only)

A curation layer on top of `generated_content` (Module 3) — see `ARCHITECTURE.md` for how `ContentDraft` relates to `GeneratedContent`.

### `POST /api/drafts`

**Request:** `{ "generatedContentId": "<uuid>", "title": "Regional Championship Recap" }` (`title` optional — defaults to `"<Content Type> Draft"`). Copies the source's `generatedText` into the new draft as `status: DRAFT`. **Errors:** `404` unknown `generatedContentId`; `400` if the source has no generated text.

### `PUT /api/drafts/{id}`

**Request:** `{ "title": "...", "contentText": "...", "status": "READY_FOR_REVIEW" }` (`status` optional, raw string — omit it to leave the status unchanged; this is also how a draft is moved back from `APPROVED` to `DRAFT`). **Errors:** `400` blank `contentText`, invalid `status` value, or the draft is soft-deleted; `409` if setting `status: APPROVED` would create a second final draft for the same source.

### `DELETE /api/drafts/{id}`

Soft-deletes (sets `deleted: true`, `deletedAt`). **Errors:** `400` if already deleted.

### `GET /api/drafts`

Paginated, searchable, filterable list. Query params: `page`, `size`, `sort`, `search` (matches title or content text, case-insensitive), `status`, `contentType`, `mediaId`, `includeDeleted` (default `false`). **Errors:** `400` invalid `status` value.

### `GET /api/drafts/{id}`

Single draft lookup (works regardless of deleted state, so a trashed draft can still be previewed before restoring).

### `POST /api/drafts/{id}/duplicate`

Creates an independent copy (`title` suffixed `" (Copy)"`, `status` reset to `DRAFT`). **Errors:** `400` if the source draft is deleted.

### `POST /api/drafts/{id}/restore`

Clears the soft-delete flag. **Errors:** `400` if the draft is not deleted.

### `POST /api/drafts/{id}/finalize`

Transitions the draft to `APPROVED`. **Errors:** `409` if already `APPROVED`/`PUBLISHED`/`ARCHIVED`, or if another non-deleted draft for the same `generatedContentId` is already `APPROVED`.

---

## Approval Workflow APIs (ADMIN only)

A review cycle on top of `content_drafts` (Module 4). Approving or rejecting also updates the underlying draft's own status (`DraftStatus.APPROVED` or back to `DraftStatus.DRAFT`) — see `ARCHITECTURE.md` for how `Approval` relates to `ContentDraft`.

### `POST /api/approval/submit`

**Request:** `{ "contentId": "<uuid>" }` (a `ContentDraft` id). Creates an `Approval` row at `PENDING_APPROVAL` and moves the draft to `READY_FOR_REVIEW`; notifies every active ADMIN. **Errors:** `404` unknown `contentId`; `400` if the draft is deleted; `409` if the draft already has a `PENDING_APPROVAL` or `READY_FOR_PUBLISH` approval outstanding.

### `POST /api/approval/approve/{id}`

**Request:** `{ "remarks": "optional note" }` (body itself is optional). Moves the approval to `READY_FOR_PUBLISH` and the draft to `APPROVED`; notifies the original submitter. **Errors:** `404` unknown id; `409` if the approval is not currently `PENDING_APPROVAL`; `400` if the underlying draft has since been deleted.

### `POST /api/approval/reject/{id}`

**Request:** `{ "remarks": "required reason" }`. Moves the approval to `REJECTED` and the draft back to `DRAFT` (so it can be edited and resubmitted); notifies the original submitter. **Errors:** `404` unknown id; `400` blank remarks; `409` if the approval is not currently `PENDING_APPROVAL`.

### `POST /api/approval/comment/{id}`

**Request:** `{ "comment": "..." }`. Adds a remark to the approval's discussion thread without changing its status; notifies whichever of the submitter/reviewer didn't post the comment. **Errors:** `404` unknown id; `400` blank comment.

### `GET /api/approval/pending`

Paginated, searchable, filterable list. Query params: `page`, `size`, `sort`, `status` (defaults to `PENDING_APPROVAL` if omitted — the same endpoint serves the Approved/Rejected dashboard tabs by passing `status=READY_FOR_PUBLISH`/`REJECTED`), `search` (matches the content title, case-insensitive), `dateFrom`/`dateTo` (ISO-8601 instants, inclusive). **Errors:** `400` invalid `status` value or malformed date.

### `GET /api/approval/history/{contentId}`

Paginated, newest-first `ApprovalHistory` timeline for one draft — every submit/approve/reject/comment ever recorded against it, across all of its approval cycles if it was rejected and resubmitted more than once.

### `GET /api/approval/{id}`

Single approval, including its full comment thread. Works regardless of the approval's status.

---

## Instagram Publisher APIs (ADMIN only)

Publishes `approvals` (Module 5) at `READY_FOR_PUBLISH` to a connected Instagram Business Account, via either the real Meta Graph API or a `dev`-only mock — see `INSTAGRAM_PUBLISHER.md` for the full design and how the two are switched purely by configuration. All endpoints under `/api/instagram/**` require the `ADMIN` role.

### `POST /api/instagram/connect`

Connects an Instagram Business Account. Verifies the credentials against the active publisher (a real Graph API call, or an instant mock success in `dev`), then stores the account with the access token **encrypted at rest** (AES-256-GCM). The raw token is never persisted in plaintext and never appears in any response.

**Request:**
```json
{
  "businessAccountId": "17841400000000000",
  "facebookPageId": "123456789",
  "accessToken": "EAAG..."
}
```

Validation: `businessAccountId` required; `accessToken` required; `facebookPageId` optional.

**Response `200`:**
```json
{
  "success": true,
  "message": "Instagram account connected",
  "data": {
    "id": "b24a1c60-...",
    "connected": true,
    "businessAccountId": "17841400000000000",
    "facebookPageId": "123456789",
    "username": "arjunsportsacademy",
    "connectedAt": "2026-07-08T17:50:53.237Z",
    "disconnectedAt": null
  },
  "timestamp": "..."
}
```

**Errors:** `409` if an account is already connected (disconnect it first — only one active connection at a time); `401` if Meta rejects the credentials (real publisher only); `503`/`504` if Meta is unreachable or times out.

### `GET /api/instagram/status`

Returns the currently-connected account, or a `connected: false` placeholder if none is connected. Never returns the access token.

**Response `200`** (not connected):
```json
{
  "success": true,
  "data": {
    "id": null,
    "connected": false,
    "businessAccountId": null,
    "facebookPageId": null,
    "username": null,
    "connectedAt": null,
    "disconnectedAt": null
  },
  "timestamp": "..."
}
```

### `POST /api/instagram/publish`

Publishes to Instagram using the **configured active publisher** (`app.instagram.active-publisher` — the real Meta Graph API in production, `mock` in `dev` unless overridden). This is the endpoint the Publishing Queue's "Publish" button calls.

**Request:**
```json
{
  "approvalId": "8619c7bc-...",
  "mediaId": "5aacbfcf-...",
  "caption": "Our shooters brought home gold this weekend!",
  "hashtags": "#shooting #academy #champions"
}
```

Validation: `approvalId` required; `mediaId` required; `caption` required, non-blank; `hashtags` optional.

**Response `200`:**
```json
{
  "success": true,
  "message": "Published to Instagram",
  "data": {
    "id": "a02fdc39-...",
    "approvalId": "8619c7bc-...",
    "instagramAccountId": "b24a1c60-...",
    "mediaId": "5aacbfcf-...",
    "caption": "Our shooters brought home gold this weekend!",
    "hashtags": "#shooting #academy #champions",
    "status": "PUBLISHED",
    "instagramMediaId": "17900000000000000",
    "permalink": "https://www.instagram.com/p/Cxxxxxxxxxx/",
    "publisherName": "meta-graph-api",
    "errorMessage": null,
    "publishedAt": "...",
    "createdAt": "...",
    "createdBy": "5355ee95-..."
  },
  "timestamp": "..."
}
```

**Errors:** `404` unknown `approvalId` or `mediaId`; `400` the approval is not currently `READY_FOR_PUBLISH`; `409` no Instagram account is connected, or this approval has already been published; `401`/`422`/`429`/`503`/`504` publisher errors (see Status Codes above) — on any publisher error, a `FAILED` post row and history entry are still persisted (see `INSTAGRAM_PUBLISHER.md`) so the attempt can be retried.

### `POST /api/instagram/publish/mock`

Identical request/response contract to `POST /api/instagram/publish`, but **always** uses the mock publisher regardless of `app.instagram.active-publisher` — lets an admin preview the full publish flow (including the failure path, via a `SIMULATE_FAILURE` marker in the caption) without touching the real Graph API, even when a real account is connected in production-like configurations. Returns `409` ("Mock publisher is not available outside the dev profile") if no mock bean is registered, i.e. outside the `dev` profile.

### `GET /api/instagram/history`

Paginated, newest-first feed of every connect/disconnect/publish attempt (success or failure) ever recorded — global, not scoped to one post. Query params: `page`, `size`, `sort`.

**Response `200`:** `data` is a `PageResponse<InstagramHistoryResponse>` — each entry has `id`, `instagramPostId` (nullable — null for `CONNECT`/`DISCONNECT`), `action` (`CONNECT`/`DISCONNECT`/`PUBLISH_ATTEMPT`/`PUBLISH_SUCCESS`/`PUBLISH_FAILURE`/`RETRY`), `status`, `publisherName`, `errorMessage`, `actorEmail`, `createdAt`.

### `DELETE /api/instagram/disconnect`

Deactivates the currently-connected account (`is_active = false`, `disconnectedAt` set) — the row is kept, not deleted, so its publish history remains attributable. **Errors:** `400` if no account is currently connected.
