# Implementation Report — Arjun Sports AI Content Agent

**Scope of this report:** Modules 0 through 6 (Foundation & Scaffolding, Authentication & Authorization, Media Upload, AI Content Generation, Content Draft Management, Approval Workflow, Instagram Publisher). No later module has been started.

---

## 1. Summary of Module 0 — Foundation & Scaffolding

Established the monorepo, the Spring Boot application skeleton, and the Next.js dashboard shell, plus the cross-cutting infrastructure every later module reuses:

- Monorepo layout (`frontend/`, `backend/`, `infra/`, root env/README/gitignore).
- Spring Boot 3.5.16 / Java 21 bootstrap with `ApiResponse<T>`/`PageResponse<T>` envelopes, `GlobalExceptionHandler`, `BaseEntity` (JPA auditing columns).
- Append-only audit log (`ActivityLog` + `AuditLogService`) and in-app notification core (`Notification` + `NotificationService`), built before any feature that uses them so the trail is complete from the first real write.
- `AppSetting` entity/repository scaffold (no service/controller yet — deferred to a future Application Settings module).
- Flyway migration `V1__init_schema.sql`.
- Next.js bootstrap: Tailwind, shadcn/ui, dark/light theme, a dashboard shell (sidebar + topbar) with a placeholder page for every planned module.
- `docker-compose.yml` (Postgres, MinIO, backend, frontend) and Dockerfiles for both apps.
- **Committed to git** on the `dev` branch (commit `6bbebc8`, "feat: complete Module 0 - Foundation & Scaffolding").

## 2. Summary of Module 1 — Authentication & Authorization

Built a complete JWT-based authentication and authorization system on top of Module 0's foundation, plus the matching frontend session/login experience:

- `User` domain model with `Role` (`ADMIN`) and `UserStatus` (`ACTIVE`/`INACTIVE`/`SUSPENDED`) enums.
- BCrypt password hashing; JWT access tokens (15 min TTL) + opaque, hash-stored refresh tokens (7 day TTL) with rotation, reuse detection, and invalidation.
- Both tokens delivered as HttpOnly, configurable Secure/SameSite cookies.
- Full auth endpoint set: login, logout, refresh, get-current-user, change-password, forgot-password, reset-password.
- Role-based authorization on a new `/api/users/**` admin surface.
- Login rate limiting (5/min/IP), audit logging of login/failed-login/logout/password-change, in-app notifications on successful login and password change.
- Environment-driven first-admin bootstrap (no hardcoded credentials anywhere).
- Next.js: login page, session-aware dashboard layout, route-guarding `proxy.ts` with silent token refresh, real logout and change-password UI wired to a real signed-in user.
- One real bug found during manual browser verification and fixed (see §13).
- **Committed to git** on the `dev` branch (commit `d08c0fa`, "feat: complete Module 1 - Authentication & Authorization").

## 3. Summary of Module 2 — Media Upload

A MinIO-backed media library sitting behind the same auth/audit/notification infrastructure Module 1 established:

- `StorageService` interface + `MinioStorageServiceImpl`, with a **dual MinioClient** setup: one client (internal Docker DNS) for uploads/deletes, a second (`app.storage.public-endpoint`) purely for signing presigned GET URLs so they resolve from the developer's browser, not just from inside the Docker network.
- `Media` entity (soft-deletable) + per-`MediaType` validation rules (allowed content-types and max size: IMAGE 10MB, VIDEO 200MB, PDF 20MB, TEXT_NOTE 2MB), enforced in `MediaValidationRules` before any bytes reach storage.
- Upload/list/delete endpoints, audit-logged, backed by Flyway migration `V3__media.sql`.
- Next.js media library page: drag-drop upload with progress, grid view, preview, delete confirmation.
- One real production bug found and fixed during manual verification (see §13).
- **Committed to git** on the `dev` branch, bundled with Module 3 (commit `976d2b5`, "feat: complete Module 2 and Module 3 - Media Upload & AI Content Generation").

## 4. Summary of Module 3 — AI Content Generation

A provider-agnostic AI generation layer producing all 17 marketing content types from uploaded media and/or free-text context, with prompts stored in the database rather than hardcoded:

- `AIProvider` interface with an `OpenAICompatibleProvider` implementation, selected by name via `AIProviderResolver` off `app.ai.active-provider` — adding Gemini/Claude/Azure OpenAI/Ollama later means one new class, no changes to `AIContentServiceImpl` or the controller.
- `PromptTemplate` entity: versioned and immutable — "editing" a template deactivates the current row and inserts a new one at `version + 1`, enforced at the database level by a partial unique index (`WHERE is_active = TRUE`) per content type.
- `PromptBuilder` resolves `{{placeholder}}` tokens (`academyName`, media description, manual notes, event/achievement/competition/training/coach fields) against both the system and user prompt.
- `GeneratedContent` (the editable content library) and an append-only `GenerationHistory` log recording every attempt, success or failure, with actor/model/latency.
- Generate/regenerate/edit/delete endpoints, rate-limited (10/min/user), audit-logged, with in-app notifications on completion and failure.
- Flyway migrations `V4__ai_content_generation.sql` and `V5__prompt_template_seed.sql` (seeds all 17 default templates).
- Next.js generation UI: content-type picker, media/context form, generate/regenerate/copy/edit/preview/delete on each result card.
- Two real bugs found and fixed — one a significant transaction-rollback data-loss bug, one a prompt-substitution defect found during Module 4's own verification pass (see §13).
- A `MockAIProvider` (`@Profile("dev")`-gated, selected by default in the `dev` profile via `app.ai.active-provider`) was added after this module shipped, once it became clear no real OpenAI key would be available for ongoing development — see the addendum at the end of this section.
- **Committed to git** on the `dev` branch, bundled with Module 2 (commit `976d2b5`); the `MockAIProvider` addendum was committed separately (commit `8419cbe`).

**Addendum — Mock AI Provider for dev/testing:** `MockAIProvider implements AIProvider` returns realistic, content-type-appropriate sample text (captions, hashtags, blog articles, reel/short-video scripts, SEO fields, etc. — all 17 types) inferred from distinctive phrases in the resolved prompt text, since `AIGenerationRequest` deliberately carries only plain prompt strings, not a `ContentType`, to keep the interface identical to a real provider's. It weaves in whatever context (notes/achievement/event details) was actually supplied, simulates realistic latency, and supports a `SIMULATE_FAILURE` marker to deliberately exercise the failure path. The bean only exists under `@Profile("dev")` — production can never select it even via misconfiguration, since it isn't in the Spring context at all outside that profile.

## 5. Summary of Module 4 — Content Draft Management

A curation/review layer on top of Module 3's raw generated content, giving an admin a place to organize, refine, and progress content through a lifecycle before a future publishing module picks it up:

- `ContentDraft` entity with a `DRAFT → READY_FOR_REVIEW → APPROVED/REJECTED → PUBLISHED/ARCHIVED` status lifecycle, created from (and denormalizing) a source `GeneratedContent` row, then independently editable from that point on.
- Cross-module references (`generatedContentId`, `mediaId`) follow the exact pattern Module 3 established: plain nullable UUID columns with `ON DELETE SET NULL`, not JPA `@ManyToOne` — deleting the source media or generated content never fails or cascades.
- Save/edit/duplicate/soft-delete/restore/finalize endpoints, plus a search+filter+paginated list built on Spring Data JPA `Specification`s (`DraftSpecifications`) to compose free-text search with optional status/content-type/media filters without a combinatorial explosion of derived query methods.
- Business rule: only one non-deleted draft per source `generatedContentId` may be `APPROVED` at a time, enforced at the service layer with a clear `409` error rather than a raw constraint violation.
- Audit logging (`CREATE`/`UPDATE`/`DELETE`/`RESTORE`/`APPROVE`) and in-app notifications (`DRAFT_SAVED`/`DRAFT_UPDATED`/`DRAFT_DELETED`/`DRAFT_RESTORED`), reusing the same `AuditLogService`/`NotificationService` interfaces with purely additive enum extensions.
- Flyway migration `V6__content_drafts.sql`.
- Next.js Draft Management page: card/list view toggle, status chips, search/filter, preview/edit drawers (shadcn `Sheet`, distinct from Module 3's centered dialogs), trash + restore, pagination — plus a "Save as Draft" button added to Module 3's `GeneratedContentCard` as the natural entry point into this module.
- Two real bugs found and fixed during this module's own implementation and verification (see §13), one of which was a defect in Module 3's already-shipped code, not new Module 4 code.
- **Committed to git** on the `dev` branch (commit `eb3a1a6`, "feat: complete Module 4 - Content Draft Management").

## 6. Summary of Module 5 — Approval Workflow

A review cycle on top of Module 4's drafts, giving an admin reviewer a formal approve/reject decision point (with discussion) before content is considered ready to publish:

- `Approval` entity with a `PENDING_APPROVAL → APPROVED/REJECTED → READY_FOR_PUBLISH` status lifecycle, created from (and denormalizing `contentTitle`/`contentType` from) a source `ContentDraft` row for search/display purposes.
- Approving directly advances the approval to `READY_FOR_PUBLISH` (there is no separate manual "mark ready" step) and moves the underlying draft to `DraftStatus.APPROVED`; rejecting moves the approval to `REJECTED` and the draft back to `DraftStatus.DRAFT` so it can be edited and resubmitted. Both transitions are applied directly via `DraftRepository` (not `DraftService`), specifically so Module 4's own audit/notification side effects aren't duplicated on top of Module 5's.
- `ApprovalHistory` (immutable append-only log, same pattern as `GenerationHistory`) records every submit/approve/reject/comment action with before/after status; `ApprovalComment` holds the reviewer/submitter discussion thread for an approval.
- Business rule "only one outstanding review request per draft" is enforced twice: once at the service layer (clear `409`) and once at the database level (`idx_approvals_one_pending_per_content`, a partial unique index) as defense in depth — the same pattern Module 3 used for one-active-prompt-template-per-type.
- Submit/approve/reject/comment/pending-list/history/detail endpoints, all class-level `@PreAuthorize("hasRole('ADMIN')")`. Submitting notifies every active admin (`UserRepository.findByRoleAndActiveTrue` — the first read-only cross-module dependency on `modules/user` since `modules/auth`); approving/rejecting/commenting notifies the original submitter (or reviewer, for a submitter's own comment) via `Approval.createdBy` — no separate "submitted by" column was needed since `BaseEntity`'s JPA-audited `createdBy` already captures it.
- Flyway migration `V7__approval_workflow.sql`.
- Next.js Approval Dashboard (`/approvals`): Pending/Approved/Rejected tabs, search-by-title, date-range filter, pagination. Detail page (`/approvals/[id]`): status header, View Draft/Approve/Reject actions, history timeline, comments panel. A "Submit for Approval" button was added to Module 4's `DraftCard` as the natural entry point into this module.
- This module's own package layout deliberately differs from Modules 2–4's flatter convention, using explicit `entity/`, `repository/`, `service/`+`service/impl/`, `controller/`, `mapper/`, `validation/` subpackages under `modules/approval/` per an explicit structural request.
- **Committed to git** on the `dev` branch (commit `6933c33`, "feat: complete Module 5 - Approval Workflow").

## 7. Summary of Module 6 — Instagram Publisher

Publishes Module 5's `READY_FOR_PUBLISH` approvals to a connected Instagram Business Account, with the real Meta Graph API and a `dev`-only mock swappable purely by configuration — see `INSTAGRAM_PUBLISHER.md` for the complete design, workflow, and operational detail; this section is a summary.

- `InstagramPublisher` interface + `MetaGraphPublisher` (real, two-step Graph API container-then-publish flow) + `MockInstagramPublisher` (`@Profile("dev")`) + `PublisherResolver` (name-based selection off `app.instagram.active-publisher`) — the exact Strategy-pattern shape Module 3 established for `AIProvider`, reused rather than redesigned, down to both mocks supporting a `SIMULATE_FAILURE` trigger for exercising the failure path in `dev`.
- `InstagramAccount` (one active connection at a time, enforced by both a service-layer check and a partial unique database index), `InstagramPost` (one row per publish *attempt*, not a single row mutated in place — a failed attempt and a later successful retry produce two independently queryable rows), and an append-only `InstagramPublishHistory` log — all three denormalizing what they need from `Approval`/`Media`/`InstagramAccount` via plain nullable UUID columns with `ON DELETE SET NULL`, the same pattern every module since Module 3 has used.
- `CredentialEncryptionUtil` (`common/util`, new): AES-256-GCM authenticated encryption for the stored access token, distinct from `SecureTokenUtil`'s one-way SHA-256 hashing used elsewhere, since a third-party access token must be decrypted again to make API calls rather than merely compared. The key is env-injected and validated at application startup, not on first use.
- Connect/status/publish/publish-mock/history/disconnect endpoints, all class-level `@PreAuthorize("hasRole('ADMIN')")`. Publishing fires `PUBLISH_STARTED`/`PUBLISH_SUCCESS`/`PUBLISH_FAILURE` notifications and `CONNECT`/`DISCONNECT`/`PUBLISH`/`RETRY` audit log entries; a `FAILED` post row, its history entry, audit log entry, and failure notification are all recorded independently of the triggering request's own transaction via `PublishTransactionHelper` (`REQUIRES_NEW`), the same escape hatch Module 3's `GenerationFailureRecorder` established — extended in this module to also cover the `PUBLISH_STARTED` notification once manual verification caught it being silently lost on the failure path (see §13).
- Next.js Instagram Dashboard (`/instagram`): connection panel (connect/disconnect with live status), publishing queue (cards for every `READY_FOR_PUBLISH` approval), a publish dialog (media picker, editable caption pre-filled from the source draft, live preview, separate Publish/Mock Publish actions), and a publish history feed.
- One real bug found and fixed during this module's own manual verification (see §13).
- **Committed to git** on the `dev` branch (see §16 for the exact commit) — **not pushed**, per instruction.

## 8. Features Completed

| Feature | Status |
|---|---|
| Login / logout / refresh / change/forgot/reset password | ✅ Complete (Module 1) |
| Role-based user management (`/api/users`) | ✅ Complete (Module 1) |
| Media upload (image/video/PDF/text-note, validated) | ✅ Complete (Module 2) |
| Media library (list/preview/delete) | ✅ Complete (Module 2) |
| AI generation of all 17 content types | ✅ Complete (Module 3) |
| DB-backed, versioned, admin-editable prompt templates | ✅ Complete (Module 3) |
| Generate/regenerate/edit/delete generated content | ✅ Complete (Module 3) |
| Generation history log | ✅ Complete (Module 3) |
| Mock AI provider for dev/testing without a real API key | ✅ Complete (Module 3 addendum) |
| Save generated content as a draft | ✅ Complete (Module 4) |
| Edit / duplicate / soft-delete / restore a draft | ✅ Complete (Module 4) |
| Draft status lifecycle (finalize / move back to draft) | ✅ Complete (Module 4) |
| Draft search, filter, sort, pagination | ✅ Complete (Module 4) |
| Submit a draft for approval | ✅ Complete (Module 5) |
| Approve / reject with remarks | ✅ Complete (Module 5) |
| Review comments thread | ✅ Complete (Module 5) |
| Approval history timeline, pending/approved/rejected dashboard with search + date range | ✅ Complete (Module 5) |
| Connect / disconnect an Instagram Business Account | ✅ Complete (Module 6) |
| Publish approved content to Instagram (real Graph API or mock, by config) | ✅ Complete (Module 6) |
| Publish preview, media selection, editable caption/hashtags | ✅ Complete (Module 6) |
| Publish history feed | ✅ Complete (Module 6) |
| Encrypted credential storage (AES-256-GCM) | ✅ Complete (Module 6) |
| Frontend forgot/reset-password pages | ❌ Not built (backend endpoints exist and are tested; no UI) |
| Application Settings UI | ❌ Not started |
| Website publishing, scheduling, analytics | ❌ Not started |

## 9. Files / Modules Created

Full, exact file-by-file diffs for every module are in git history (`git show <commit>`) and are not reproduced line-by-line here to avoid this report drifting out of sync with the code. Package-level summary:

```
backend/src/main/java/.../storage/                 StorageService, MinioConfig (dual client), MinioStorageServiceImpl,
                                                     MinioBucketInitializer, StorageProperties            (Module 2)
backend/src/main/java/.../modules/media/            Media, MediaType, MediaValidationRules, MediaRepository,
                                                     MediaService(+Impl), MediaController, dto/            (Module 2)
backend/src/main/java/.../modules/ai/               ContentType, GenerationStatus, GenerationAction, PromptTemplate,
                                                     GeneratedContent, GenerationHistory, PromptRepository,
                                                     GeneratedContentRepository, GenerationHistoryRepository,
                                                     PromptBuilder, PromptValidator, AIResponseParser,
                                                     ContentGenerator, GenerationContext, GenerationRateLimiter,
                                                     GenerationFailureRecorder, AIContentService(+Impl),
                                                     PromptService(+Impl), AIController, PromptController, dto/ (Module 3)
backend/src/main/java/.../modules/ai/provider/      AIProvider, AIGenerationRequest/Result, AIProviderException,
                                                     AIProviderProperties, OpenAICompatibleProvider,
                                                     AIProviderResolver                                    (Module 3)
                                                     MockAIProvider                          (Module 3 addendum)
backend/src/main/java/.../modules/draft/            DraftStatus, ContentDraft, DraftRepository, DraftSpecifications,
                                                     DraftValidator, DraftMapper, DraftService(+Impl),
                                                     DraftController, dto/                                 (Module 4)
backend/src/main/java/.../modules/approval/         entity/ (Approval, ApprovalHistory, ApprovalComment,
                                                       ApprovalStatus, ApprovalAction)
                                                     repository/ (ApprovalRepository +Specifications,
                                                       ApprovalHistoryRepository, ApprovalCommentRepository)
                                                     service/ + service/impl/ (ApprovalService/Impl)
                                                     controller/ (ApprovalController)
                                                     mapper/ (ApprovalMapper), validation/ (ApprovalValidator)
                                                     dto/                                                  (Module 5)
backend/src/main/java/.../modules/instagram/        entity/ (InstagramAccount, InstagramPost, InstagramPostStatus,
                                                       InstagramPublishHistory, InstagramHistoryAction)
                                                     provider/ (InstagramPublisher, MetaGraphPublisher,
                                                       MockInstagramPublisher, PublisherResolver,
                                                       InstagramPublisherException, InstagramPublisherProperties,
                                                       InstagramAccountInfo, InstagramPublishRequest/Result)
                                                     repository/ (InstagramAccountRepository, InstagramPostRepository,
                                                       InstagramPublishHistoryRepository)
                                                     service/ + service/impl/ (InstagramService/Impl,
                                                       PublishTransactionHelper)
                                                     controller/ (InstagramController)
                                                     mapper/ (InstagramMapper), validation/ (InstagramValidator)
                                                     dto/, InstagramAccountBootstrapRunner                 (Module 6)
backend/src/main/java/.../common/util/              CredentialEncryptionUtil (AES-256-GCM)                (Module 6)
backend/src/main/resources/db/migration/            V3__media.sql, V4__ai_content_generation.sql,
                                                     V5__prompt_template_seed.sql, V6__content_drafts.sql,
                                                     V7__approval_workflow.sql, V8__instagram_publisher.sql
backend/src/test/java/.../modules/media/            MediaServiceImplTest
backend/src/test/java/.../modules/ai/               PromptBuilderTest, PromptValidatorTest, AIResponseParserTest,
                                                     AIContentServiceImplTest, AIControllerIntegrationTest
backend/src/test/java/.../modules/ai/provider/      MockAIProviderTest
backend/src/test/java/.../modules/draft/            DraftServiceImplTest, DraftControllerIntegrationTest,
                                                     DraftRepositoryTest
backend/src/test/java/.../modules/approval/         ApprovalServiceImplTest, ApprovalControllerIntegrationTest
backend/src/test/java/.../modules/instagram/        InstagramServiceImplTest, InstagramControllerIntegrationTest
backend/src/test/java/.../modules/instagram/provider/ MockInstagramPublisherTest
backend/src/test/java/.../common/util/              CredentialEncryptionUtilTest
frontend/src/components/media/                      upload-dropzone, media-card, media-grid
frontend/src/components/ai/                          generation-form, generated-content-card(+list)
frontend/src/components/drafts/                      draft-create-form, draft-filters, draft-card, draft-list,
                                                     draft-preview-sheet, draft-edit-sheet
frontend/src/components/approvals/                   approval-filters, approval-queue(-card), approve/reject-dialog,
                                                     view-draft-dialog, approval-history-timeline,
                                                     approval-comments-panel
frontend/src/components/instagram/                  connection-panel, publishing-queue, publish-dialog,
                                                     instagram-history-list
frontend/src/app/(dashboard)/media/page.tsx          (Module 2), .../content/page.tsx (Module 3),
                                                     .../drafts/page.tsx (Module 4),
                                                     .../approvals/page.tsx + approvals/[id]/page.tsx (Module 5),
                                                     .../instagram/page.tsx (Module 6)
                                                     — all rewritten from placeholders
frontend/src/lib/api/{media,ai,drafts,approval,instagram}.ts   typed API clients per module
frontend/src/types/{media,ai,drafts,approval,instagram}.ts     shared frontend types per module
```

Additive-only changes to already-shipped modules (every one of these is a new enum constant, new optional config key, or new independent method — no existing behavior was altered):

```
common/audit/ActivityAction.java            + RESTORE                                     (Module 4)
                                             + SUBMIT, COMMENT                              (Module 5)
                                             + CONNECT, DISCONNECT, RETRY                   (Module 6)
common/notification/NotificationType.java   + GENERATION_COMPLETED, GENERATION_FAILED      (Module 3)
                                             + DRAFT_SAVED, DRAFT_UPDATED, DRAFT_DELETED,
                                               DRAFT_RESTORED                               (Module 4)
                                             + APPROVAL_COMMENT_ADDED (APPROVAL_REQUIRED/
                                               APPROVED/REJECTED existed since Module 0/1,
                                               unused until now)                            (Module 5)
                                             + PUBLISH_STARTED (PUBLISH_SUCCESS/FAILURE
                                               existed since Module 0, unused until now)    (Module 6)
common/exception/RateLimitExceededException.java   new (Module 3)
common/exception/GlobalExceptionHandler.java + RateLimitExceededException, AIProviderException handlers (Module 3)
                                             + InstagramPublisherException handler          (Module 6)
modules/user/UserRepository.java             + findByRoleAndActiveTrue                      (Module 5)
application.yml / docker-compose.yml / .env.example  + app.storage.*, app.ai.* config blocks (Modules 2–3)
                                             + app.security.encryption-key, app.instagram.*  (Module 6)
application-dev.yml                          + app.ai.active-provider defaulting to "mock"   (Module 3 addendum)
                                             + app.instagram.active-publisher defaulting to
                                               "mock" regardless of other configuration      (Module 6)
frontend/src/lib/api/client.ts               + PageResponse<T>, apiFetchFormData()          (Module 2)
frontend/src/lib/nav-config.ts               Media/AI/Drafts/Approvals/Instagram flipped "upcoming" → "available" as each shipped
frontend/src/components/ai/generated-content-card.tsx  + "Save as Draft" button              (Module 4, integration point)
frontend/src/components/drafts/draft-card.tsx          + "Submit for Approval" button        (Module 5, integration point)
```

## 10. Database Changes

| Migration | Module | Adds |
|---|---|---|
| `V3__media.sql` | 2 | `media` |
| `V4__ai_content_generation.sql` | 3 | `prompt_templates`, `generated_content`, `generation_history` |
| `V5__prompt_template_seed.sql` | 3 | 17 seed rows in `prompt_templates` (one per content type) |
| `V6__content_drafts.sql` | 4 | `content_drafts` |
| `V7__approval_workflow.sql` | 5 | `approvals`, `approval_history`, `approval_comments` |
| `V8__instagram_publisher.sql` | 6 | `instagram_accounts`, `instagram_posts`, `instagram_publish_history` |

Full column-by-column detail, indexes, and an ASCII ER diagram are in `DATABASE_DESIGN.md`. All new foreign keys use `ON DELETE SET NULL` (except `approval_comments`, deliberately `CASCADE` — see `DATABASE_DESIGN.md` §2), so deleting upstream media/content/drafts/approvals never cascades or fails unexpectedly — see `ARCHITECTURE.md` §15.

## 11. API Endpoints Delivered

In addition to the Module 0/1 endpoints listed in earlier versions of this report (health, auth, users — unchanged, see `API_DOCUMENTATION.md`):

| Method | Path | Auth | Module |
|---|---|---|---|
| POST | `/api/media/upload` | Authenticated | 2 |
| GET | `/api/media` | Authenticated | 2 |
| DELETE | `/api/media/{id}` | Authenticated | 2 |
| POST | `/api/ai/generate` | ADMIN | 3 |
| POST | `/api/ai/regenerate` | ADMIN | 3 |
| GET | `/api/ai/history` | ADMIN | 3 |
| GET / PUT / DELETE | `/api/ai/content`, `/api/ai/content/{id}` | ADMIN | 3 |
| GET | `/api/prompts` | ADMIN | 3 |
| PUT | `/api/prompts/{id}` | ADMIN | 3 |
| POST | `/api/drafts` | ADMIN | 4 |
| PUT | `/api/drafts/{id}` | ADMIN | 4 |
| DELETE | `/api/drafts/{id}` | ADMIN | 4 |
| GET | `/api/drafts` | ADMIN | 4 |
| GET | `/api/drafts/{id}` | ADMIN | 4 |
| POST | `/api/drafts/{id}/duplicate` | ADMIN | 4 |
| POST | `/api/drafts/{id}/restore` | ADMIN | 4 |
| POST | `/api/drafts/{id}/finalize` | ADMIN | 4 |
| POST | `/api/approval/submit` | ADMIN | 5 |
| POST | `/api/approval/approve/{id}` | ADMIN | 5 |
| POST | `/api/approval/reject/{id}` | ADMIN | 5 |
| POST | `/api/approval/comment/{id}` | ADMIN | 5 |
| GET | `/api/approval/pending` | ADMIN | 5 |
| GET | `/api/approval/history/{contentId}` | ADMIN | 5 |
| GET | `/api/approval/{id}` | ADMIN | 5 |
| POST | `/api/instagram/connect` | ADMIN | 6 |
| GET | `/api/instagram/status` | ADMIN | 6 |
| POST | `/api/instagram/publish` | ADMIN | 6 |
| POST | `/api/instagram/publish/mock` | ADMIN | 6 |
| GET | `/api/instagram/history` | ADMIN | 6 |
| DELETE | `/api/instagram/disconnect` | ADMIN | 6 |

Full request/response contracts, validation rules, and error codes are documented in `API_DOCUMENTATION.md`.

## 12. Testing Performed

### Automated (backend, JUnit 5 + Mockito + Testcontainers)

**157 tests, all passing**, across the full suite (`cd backend && mvn test`):

| Test class | Count | Module |
|---|---|---|
| `JwtTokenProviderTest`, `RefreshTokenServiceImplTest`, `AuthServiceImplTest`, `AuthControllerIntegrationTest`, `AuditLogServiceImplTest`, `InAppNotificationServiceImplTest` | 4, 6, 4, 7, 2, 6 | 0/1 |
| `MediaServiceImplTest` | 6 | 2 |
| `PromptBuilderTest`, `PromptValidatorTest`, `AIResponseParserTest`, `AIContentServiceImplTest`, `AIControllerIntegrationTest`, `MockAIProviderTest` | 2, 5, 5, 6, 6, 7 | 3 |
| `DraftServiceImplTest`, `DraftControllerIntegrationTest`, `DraftRepositoryTest` | 16, 13, 5 | 4 |
| `ApprovalServiceImplTest`, `ApprovalControllerIntegrationTest` | 12, 12 | 5 |
| `CredentialEncryptionUtilTest` | 6 | 6 |
| `MockInstagramPublisherTest` | 4 | 6 |
| `InstagramServiceImplTest`, `InstagramControllerIntegrationTest` | 11, 12 | 6 |

Notable regression tests: `AIControllerIntegrationTest.generate_providerFailure_returns503AndPersistsFailedHistoryDespiteRollback` (guards the transaction-rollback bug in §13), `DraftControllerIntegrationTest.finalize_thenFinalizeAnotherDraftOfSameSource_returnsConflict` (guards the duplicate-final-draft business rule), `ApprovalControllerIntegrationTest.approve_alreadyApproved_returnsConflict`/`submit_duplicatePending_returnsConflict` (guard the one-outstanding-review-per-draft rule at the API level, backed by both the service-layer check and the database's partial unique index), and `InstagramServiceImplTest.publish_success_savesPublishedPostAndNotifies`/`publish_providerFailure_recordsFailureAndRethrows` (both now assert `PublishTransactionHelper.notifyStarted(...)` was called, guarding the notification-rollback bug in §13).

### Manual / interactive (frontend + full stack)

No automated frontend test runner is configured (no Jest/RTL/Playwright dependency committed to `package.json`); Modules 2 through 5 were each verified with a one-off Playwright/Chromium session (installed ad hoc, not part of the committed project) driving the real Docker stack:

- **Module 2**: upload (image/video/PDF/text-note) → grid renders → preview → delete, against real MinIO.
- **Module 3**: generate → regenerate → copy → edit → toggle draft/final → delete, against both a local mock OpenAI-compatible endpoint and the real OpenAI API (auth-failure path, to prove error handling against a genuine external response); the `MockAIProvider` addendum was separately verified end-to-end against the real running stack (multiple content types, the `SIMULATE_FAILURE` trigger, and a full generate → save-as-draft round trip) with zero external calls.
- **Module 4**: save draft → preview drawer → edit drawer → finalize → duplicate → search filters correctly → delete → show trash → restore, plus a second pass specifically exercising the duplicate-final-draft `409` and confirming it now renders as an inline error rather than an uncaught exception (see §13).
- **Module 5**: submit → pending queue shows it → detail page loads → View Draft shows live draft content → add comment → history timeline reflects it → approve with remarks → status becomes Ready for Publish → item disappears from the Pending tab and appears under Approved. A separate curl pass exercised reject (draft correctly returns to `DRAFT`), the duplicate-submission `409`, the already-approved `409`, and confirmed audit log + notification rows were created for every action.
- **Module 6**: no interactive browser session was available in this environment for this module (no Playwright/Chromium tooling was set up this session) — verification was instead done via an exhaustive curl pass plus direct database inspection: connect (token-never-exposed and encrypted-at-rest confirmed via direct `psql` query) → status → publish (success) → publish/mock → publish with `SIMULATE_FAILURE` (`503`, `FAILED` row persisted) → retry with a corrected caption (succeeds, logged as `RETRY` not `PUBLISH`) → duplicate-publish attempt (`409`, no duplicate row) → disconnect → publish-while-disconnected (`409`) → reconnect. `instagram_posts`, `instagram_publish_history`, `activity_log`, and `notifications` row counts were all directly verified against the exact sequence of calls made, which is what caught the bug in §13. `npm run build` compiles the Instagram dashboard and its components cleanly and `npm run lint` is clean, but the click-through UI itself (connection panel, publish dialog, queue, history feed) has **not** been visually confirmed in a real browser — flagged here explicitly rather than assumed working, per the instruction to say so when UI verification isn't possible.

All Playwright sessions (Modules 2–5) completed with zero uncaught client-side exceptions on their final run. `npm run lint` and `npm run build` are clean after every module, including Module 6.

## 13. Bugs Found and Fixed

### Profile dropdown crash ("This page couldn't load") — Module 1

- **Symptom:** clicking the user icon in the topbar to open the account dropdown crashed the page with Next.js's generic client-error screen.
- **Root cause:** shadcn's `DropdownMenuLabel` (built on `@base-ui/react`'s `Menu.GroupLabel`) reads a React context only provided by `Menu.Group`; it was rendered without a `DropdownMenuGroup` wrapper.
- **Fix:** wrapped the label in `<DropdownMenuGroup>` in `frontend/src/components/layout/topbar.tsx`.

### MinIO presigned URLs used the Docker-internal hostname — Module 2

- **Symptom:** uploaded media previewed fine from inside a container's own network but returned an unreachable URL (`http://minio:9000/...`) to the browser.
- **Root cause:** a single `MinioClient` bean was used for both upload operations and presigned URL signing; the signature is generated against whatever endpoint the client is configured with, and that endpoint (`http://minio:9000`) only resolves on the Docker Compose network, not on the developer's machine.
- **Fix:** introduced a second `MinioClient` bean (`MinioConfig.PUBLIC_URL_CLIENT`) configured with a distinct `STORAGE_PUBLIC_ENDPOINT`, used only for `presignedGetUrl()`; all other operations keep using the internal-endpoint client.

### Failed AI generations silently lost their audit trail on rollback — Module 3

- **Symptom:** discovered via manual Docker verification (not caught by unit tests, since they don't exercise real Spring transactions): when generation failed, the `FAILED` history row, audit log entry, and failure notification were all being written *inside* the same `@Transactional` method that then re-threw the exception — causing Spring to roll back the entire transaction, silently erasing the very failure record meant to explain what went wrong.
- **Fix:** extracted the failure-recording logic into `GenerationFailureRecorder`, a separate bean whose method runs with `@Transactional(propagation = Propagation.REQUIRES_NEW)`, committing independently of the caller's (rolled-back) transaction.
- **Regression test:** `AIControllerIntegrationTest.generate_providerFailure_returns503AndPersistsFailedHistoryDespiteRollback`.

### System prompts never resolved the `{{academyName}}` placeholder — found during Module 4 verification, bug was in Module 3

- **Symptom:** found while raw-capturing an outgoing AI request during Module 4's manual verification pass — every request's system prompt contained the literal string `{{academyName}}` instead of "Arjun Sports Shooting Academy", across all 17 content types.
- **Root cause:** `PromptBuilder.build()` substituted placeholders into the resolved user prompt but returned `template.getSystemPrompt()` completely unmodified.
- **Fix:** `PromptBuilder` now resolves placeholders in both the system and user prompt via a shared `resolve()` helper. `PromptBuilderTest` updated to assert on a system prompt containing a placeholder (the previous test's fixture had no placeholder in the system prompt, which is why it didn't catch this).

### Draft card actions had no error handling — Module 4

- **Symptom:** found via Playwright — attempting to finalize a draft whose source already had another `APPROVED` draft correctly returned `409` from the backend, but the frontend's `handleFinalize`/`handleDuplicate`/`handleRestore`/`handleMoveToDraft` had no `try`/`catch`, so the rejection surfaced as an uncaught promise rejection instead of a visible message.
- **Fix:** `DraftCard`'s shared `withBusy()` wrapper now catches and displays the error inline (matching the pattern already used by the create/edit forms); added a dedicated Playwright check asserting the conflict renders as an inline `Alert`, not a crash.

No bugs were found during Module 5's implementation or verification — its curl and Playwright passes both succeeded on the first attempt.

### `PUBLISH_STARTED` notification silently lost on a failed publish — Module 6

- **Symptom:** found via a deliberate before/after row-count check on `notifications WHERE type = 'PUBLISH_STARTED'` around a deliberately-failed publish attempt (using the mock publisher's `SIMULATE_FAILURE` caption trigger) — the count did not increase, even though `doPublish()` unconditionally fires that notification before ever calling the publisher.
- **Root cause:** the same category of bug as §13's Module 3 entry, in a new module: the started-notification was written inside `doPublish()`'s own `@Transactional` method. When the publisher call failed and the method re-threw, Spring rolled back the entire transaction — including the notification insert that had logically already happened earlier in the same method body. The failure path ended up with a `PUBLISH_FAILURE` notification but no paired `PUBLISH_STARTED` one.
- **Fix:** extracted the started-notification into `PublishTransactionHelper.notifyStarted(...)`, a `@Transactional(propagation = REQUIRES_NEW)` method alongside the pre-existing failure-recording method on the same bean (renamed from `PublishFailureRecorder` to `PublishTransactionHelper` to reflect its now-dual responsibility). `doPublish()` calls the helper instead of the notification service directly, so the insert commits immediately and independently of whatever happens afterward in the same request.
- **Regression tests:** `InstagramServiceImplTest.publish_success_savesPublishedPostAndNotifies` and `publish_providerFailure_recordsFailureAndRethrows` both now assert `notifyStarted(...)` was called on the helper. Full detail (including the exact before/after counts observed) is in `INSTAGRAM_PUBLISHER.md` §11.

## 14. Security Features Implemented

Carried over from Module 1 (BCrypt hashing, JWT + refresh token rotation/reuse-detection, HttpOnly/Secure/SameSite cookies, CORS with explicit origins, role-based `@PreAuthorize`, login rate limiting, full Bean Validation, audit trail, no hardcoded credentials) — see earlier versions of this report or `ARCHITECTURE.md` §9 for detail. Added in Modules 2–6:

- Per-`MediaType` upload validation (content-type allow-list + max size) enforced before any bytes are persisted to storage.
- AI generation rate limiting (Bucket4j, 10/min/user), independent of the login limiter.
- `/api/ai/**`, `/api/prompts/**`, `/api/drafts/**`, `/api/approval/**`, and `/api/instagram/**` are all class-level `@PreAuthorize("hasRole('ADMIN')")` — no endpoint under these paths is reachable by an authenticated-but-non-admin user (satisfying Module 5's explicit "Only ADMIN can approve" rule and Module 6's explicit "ADMIN only" rule, both applied consistently to the whole controller rather than just one action).
- AI provider errors are classified (`AIProviderException.Reason`) and mapped to distinct HTTP statuses rather than leaking raw provider error bodies to the client; Module 6's `InstagramPublisherException.Reason` does the same for Meta Graph API errors (`ARCHITECTURE.md` §9, `INSTAGRAM_PUBLISHER.md` §7).
- `MockAIProvider`/`MockInstagramPublisher` are both unreachable outside the `dev` Spring profile at the bean-registration level (`@Profile("dev")`), not just by configuration default — a production misconfiguration cannot select a provider that was never instantiated.
- Instagram access tokens are encrypted at rest with AES-256-GCM (`CredentialEncryptionUtil`) before being stored, decrypted only in memory immediately before a publish call, and never included in any response DTO — verified directly via `psql` during manual testing (§12), not just asserted by code inspection.

## 15. Verification Results

| Check | Result |
|---|---|
| `mvn test` (backend, full suite) | ✅ 157/157 passing |
| `npm run lint` (frontend) | ✅ Clean |
| `npm run build` (frontend) | ✅ Clean, all routes compiled (incl. `/approvals`, `/approvals/[id]`, `/instagram`) |
| `docker compose up` (full stack) | ✅ All 4 containers healthy/running |
| Flyway migrations `V1`–`V8` | ✅ Applied cleanly against a real Postgres container |
| Swagger UI / `/api-docs` (dev profile) | ✅ Loads, lists all 38 documented endpoints across Modules 1–6 |
| Media upload → preview → delete (real MinIO) | ✅ Verified via curl and browser |
| AI generation success/failure paths | ✅ Verified against a mock endpoint and the real OpenAI API |
| Draft save/edit/duplicate/finalize/delete/restore/search/filter | ✅ Verified via curl and browser (Playwright, zero uncaught errors on final run) |
| Duplicate-final-draft business rule | ✅ `409` verified via curl and browser, with graceful inline error display |
| Approval submit/approve/reject/comment/history/pending | ✅ Verified via curl and browser (Playwright, zero uncaught errors on first run) |
| Duplicate-pending-approval and already-approved business rules | ✅ `409` verified via curl at both the service layer and the database's partial unique index |
| Draft ↔ Approval status sync (submit → READY_FOR_REVIEW, approve → APPROVED, reject → DRAFT) | ✅ Verified via curl and direct DB inspection |
| Audit log + notifications on every approval action | ✅ Verified via direct DB inspection (`activity_log`, `notifications`) |
| Instagram connect/status/publish/publish-mock/history/disconnect | ✅ Verified via curl and direct DB inspection (see `INSTAGRAM_PUBLISHER.md` §10) — **not** verified in an interactive browser session this module (no Playwright tooling available this session; see §12) |
| Instagram access token encrypted at rest, never returned by any endpoint | ✅ Verified via direct `psql` inspection of `access_token_encrypted` and a `grep` over every JSON response for the raw token value |
| Instagram publish failure/retry/duplicate-publish business rules | ✅ `503`/`200`/`409` verified via curl, with `instagram_posts`/`instagram_publish_history`/`activity_log`/`notifications` row counts confirmed against the exact call sequence |

## 16. Current Project Status

- **Module 0**: complete, committed (`dev`, commit `6bbebc8`).
- **Module 1**: complete, committed (`dev`, commit `d08c0fa`).
- **Modules 2 & 3**: complete, committed together (`dev`, commit `976d2b5`).
- **Module 4**: complete, committed (`dev`, commit `eb3a1a6`). Its Mock AI Provider addendum is committed separately (`dev`, commit `8419cbe`).
- **Module 5**: complete, committed (`dev`, commit `6933c33`).
- **Module 6**: complete and verified as described above; committed to `dev` (see the commit created alongside this report) — **not pushed**, per instruction.
- The full stack runs locally via `docker compose --env-file .env -f infra/docker-compose.yml up -d` on ports 3000 (frontend), 8080 (backend), 5433 (Postgres, host-mapped), 9000/9001 (MinIO).

## 17. Remaining Roadmap

Per the agreed build plan, not started yet:

7. Website Publisher
8. Scheduler
9. Content History & Analytics
10. Activity Log Viewer (UI over the audit trail that has been recording since Module 0)
11. Notification Center (UI over the in-app notifications already being written)
12. Security Hardening & Finalization
13. DevOps & Docs Finalization

Also still outstanding: Application Settings (full CRUD + admin UI over the `AppSetting` scaffold from Module 0), frontend forgot-password/reset-password pages (backend ready, no UI), and real email delivery for password reset. A real OpenAI (or other provider) API key is still not configured — `MockAIProvider` covers all dev/demo needs, but no content has been generated by an actual AI model.
