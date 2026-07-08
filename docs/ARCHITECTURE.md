# Architecture — Arjun Sports AI Content Agent

**Status:** Reflects Modules 0 through 6 as actually implemented. No feature described below exists only as a plan; anything not yet built is explicitly called out as "not yet implemented."

---

## 1. Project Overview

Arjun Sports AI Content Agent is an AI-assisted content marketing system being built for Arjun Sports Shooting Academy. The intended end state (not all delivered yet) is: upload media → generate AI content (Instagram captions, blog posts, SEO metadata, reel scripts) → review and approve it → publish to Instagram and the academy website → track history on a schedule.

The system is delivered module-by-module. As of this document, six modules are complete:

- **Module 0 — Foundation & Scaffolding**: monorepo layout, Spring Boot bootstrap, cross-cutting audit logging and in-app notifications, a settings entity scaffold, and a Next.js dashboard shell.
- **Module 1 — Authentication & Authorization**: a complete JWT-based auth system (login, logout, refresh with rotation, current-user, change password, forgot/reset password architecture), role-based authorization, and the matching Next.js login/session UI.
- **Module 2 — Media Upload**: MinIO-backed storage with per-type validation and a Next.js media library.
- **Module 3 — AI Content Generation**: a provider-agnostic AI generation layer producing all 17 content types from DB-backed, versioned prompt templates.
- **Module 4 — Content Draft Management**: a curation/review layer on top of Module 3's raw generated content.
- **Module 5 — Approval Workflow**: a review cycle on top of Module 4's drafts.
- **Module 6 — Instagram Publisher**: connects an Instagram Business Account and publishes `READY_FOR_PUBLISH` approvals to it, via a Strategy-pattern publisher abstraction swappable between the real Meta Graph API and a `dev`-only mock.

## 2. Goals and Objectives

- Provide a secure, single-admin (extensible to multi-role) authentication system as the foundation every later module builds on.
- Keep the codebase production-grade from the first commit: no `ddl-auto: update`, no plaintext passwords, no hardcoded credentials, every schema change versioned via Flyway.
- Establish cross-cutting infrastructure (audit trail, notifications) once, in Module 0, so every subsequent module (including Module 1) participates in it from day one rather than bolting it on later.
- Keep the frontend and backend independently deployable but tightly contracted via a typed API response envelope.

## 3. High-Level Architecture

```
┌─────────────────────┐        HTTPS/HTTP         ┌──────────────────────┐
│   Browser (Admin)    │ ─────────────────────────▶ │  Next.js Frontend    │
│                      │ ◀───────────────────────── │  (App Router, :3000) │
└─────────────────────┘        HttpOnly cookies     └──────────┬───────────┘
                                                                 │ REST (JSON)
                                                                 │ credentials: include
                                                                 ▼
                                                      ┌──────────────────────┐
                                                      │  Spring Boot API     │
                                                      │  (:8080)             │
                                                      └──────────┬───────────┘
                                                                 │ JDBC
                                                                 ▼
                                                      ┌──────────────────────┐
                                                      │  PostgreSQL 16       │
                                                      └──────────────────────┘

                                                      ┌──────────────────────┐
                                                      │  MinIO (S3-compat)   │
                                                      │  provisioned, not    │
                                                      │  yet integrated      │
                                                      └──────────────────────┘
```

The frontend never talks to PostgreSQL or MinIO directly — every data access goes through the Spring Boot REST API.

## 4. Technology Stack (as installed)

### Backend
- Java 21, Spring Boot 3.5.16 (Maven, `spring-boot-starter-parent`)
- `spring-boot-starter-web`, `-validation`, `-actuator`, `-security`, `-data-jpa`
- PostgreSQL JDBC driver, Flyway 10.x (`flyway-core` + `flyway-database-postgresql`)
- JJWT 0.13.0 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) for signed access tokens
- Bucket4j 8.10.1 for in-memory login rate limiting
- springdoc-openapi 2.8.17 (Swagger UI / OpenAPI 3, Spring Boot 3-compatible line)
- Lombok, MapStruct 1.6.3 (MapStruct is on the classpath as a dependency; no generated mapper is in use yet — DTO conversion today is hand-written, e.g. `UserResponse.from(User)`)
- io.minio:minio 9.0.3 (dependency present for the future Media Upload module; **no `StorageService` or MinIO client code exists yet**)
- Test: `spring-boot-starter-test`, `spring-security-test`, `spring-boot-testcontainers`, Testcontainers `junit-jupiter` + `postgresql`

### Frontend
- Next.js 16.2.10 (App Router, Turbopack), React 19.2.4, TypeScript 5
- Tailwind CSS 4, shadcn/ui components built on `@base-ui/react` (not Radix)
- `next-themes` 0.4.6 for dark/light mode
- `lucide-react` for icons

### Database
- PostgreSQL 16 (Docker: `postgres:16-alpine`), schema managed exclusively by Flyway migrations

### Storage
- MinIO (`minio/minio:latest`) — container runs in `docker-compose.yml`; **not yet wired into any backend service**

### Deployment
- Docker Compose (`infra/docker-compose.yml`): postgres, minio, backend, frontend
- Multi-stage Dockerfiles for both backend (Maven build → `eclipse-temurin:21-jre-alpine`) and frontend (`next build --output standalone` → `node:22-alpine`)

## 5. Monorepo Structure

```
ContenAgent/
├── backend/            Spring Boot API (Maven)
├── frontend/           Next.js admin dashboard
├── infra/              docker-compose.yml
├── docs/               This documentation set
├── .env.example        Full list of environment variables
└── README.md
```

## 6. Backend Architecture

Package root: `com.arjunsports.contentagent`. The backend follows a layered, package-by-feature structure:

```
com.arjunsports.contentagent
├── ContentAgentApplication.java     @SpringBootApplication, @EnableJpaAuditing, @EnableAsync, @EnableScheduling
├── common/                          Cross-cutting code shared by every module
│   ├── HealthController.java        GET /api/health
│   ├── audit/                       Append-only audit trail (Module 0, extended in Module 1)
│   ├── dto/                         ApiResponse<T>, PageResponse<T> — every endpoint returns these
│   ├── entity/                      BaseEntity (id, createdAt, updatedAt, createdBy, updatedBy)
│   ├── exception/                   Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
│   ├── notification/                In-app notification core (Module 0, used by Module 1)
│   ├── security/                    AuthenticatedActor marker interface
│   ├── util/                        SecureTokenUtil (opaque token generation/hashing),
│   │                                 CredentialEncryptionUtil (AES-256-GCM, Module 6)
│   └── validation/                  Shared Bean Validation regex constants
├── config/                          SecurityConfig, CorsConfig, JpaAuditingConfig, OpenApiConfig
├── modules/
│   ├── auth/                        Login/logout/refresh/change-password/forgot-reset password
│   ├── user/                        User entity, roles, admin bootstrap, user CRUD (admin-only)
│   ├── settings/                    AppSetting entity scaffold only (no service/controller yet)
│   ├── media/                       Media entity + validation rules, upload/list/delete (Module 2)
│   ├── ai/                          PromptTemplate/GeneratedContent/GenerationHistory, AIProvider abstraction (Module 3)
│   │   ├── provider/                AIProvider interface + OpenAICompatibleProvider, resolver, exceptions
│   │   └── dto/
│   ├── draft/                       ContentDraft: save/edit/duplicate/restore/finalize workflow (Module 4)
│   │   └── dto/
│   ├── approval/                    Approval workflow on top of ContentDraft (Module 5)
│   │   ├── entity/                  Approval, ApprovalHistory, ApprovalComment, ApprovalStatus, ApprovalAction
│   │   ├── repository/               ApprovalRepository (+Specifications), ApprovalHistoryRepository, ApprovalCommentRepository
│   │   ├── service/ + service/impl/  ApprovalService / ApprovalServiceImpl
│   │   ├── controller/               ApprovalController
│   │   ├── mapper/                   ApprovalMapper
│   │   ├── validation/               ApprovalValidator
│   │   └── dto/
│   └── instagram/                   Instagram publishing on top of Approval (Module 6)
│       ├── entity/                  InstagramAccount, InstagramPost, InstagramPostStatus,
│       │                             InstagramPublishHistory, InstagramHistoryAction
│       ├── provider/                 InstagramPublisher interface + MetaGraphPublisher,
│       │                             MockInstagramPublisher (@Profile("dev")), PublisherResolver,
│       │                             InstagramPublisherException, InstagramPublisherProperties
│       ├── repository/               InstagramAccountRepository, InstagramPostRepository,
│       │                             InstagramPublishHistoryRepository
│       ├── service/ + service/impl/  InstagramService / InstagramServiceImpl, PublishTransactionHelper
│       ├── controller/               InstagramController
│       ├── mapper/                   InstagramMapper
│       ├── validation/               InstagramValidator
│       ├── dto/
│       └── InstagramAccountBootstrapRunner   optional env-driven auto-connect on startup
├── storage/                         StorageService interface + MinioStorageServiceImpl, dual-endpoint MinioConfig (Module 2)
└── security/                        JWT provider, cookie handling, filters, rate limiter
```

Each feature module under `modules/` follows Repository Pattern: `entity → repository → service (interface + impl) → controller → dto`. Controllers never touch repositories directly. Modules 5 and 6 are the exception to the otherwise-flat per-module package layout Modules 2–4 use (`entity/`, `repository/`, `service/`+`service/impl/`, `controller/`, `mapper/`, `validation/` as explicit subpackages rather than files directly under `modules/approval/`/`modules/instagram/`) — an explicit, deliberate structural choice for these modules, not an inconsistency. Module 6 additionally has a `provider/` subpackage, the same Strategy-pattern shape `modules/ai/provider/` established in Module 3 (interface + real impl + `dev`-only mock impl + a resolver bean).

### Design patterns actually in use

| Pattern | Where | Why |
|---|---|---|
| Repository | `UserRepository`, `RefreshTokenRepository`, `PasswordResetTokenRepository`, `ActivityLogRepository`, `NotificationRepository`, `AppSettingRepository`, `MediaRepository`, `PromptRepository`, `GeneratedContentRepository`, `GenerationHistoryRepository`, `DraftRepository`, `ApprovalRepository`, `ApprovalHistoryRepository`, `ApprovalCommentRepository`, `InstagramAccountRepository`, `InstagramPostRepository`, `InstagramPublishHistoryRepository` | Spring Data JPA abstracts persistence behind an interface per aggregate. |
| Service interface + impl | `AuthService`, `UserService`, `RefreshTokenService`, `AuditLogService`, `NotificationService`, `MediaService`, `AIContentService`, `PromptService`, `DraftService`, `ApprovalService`, `InstagramService` (each with a matching `*Impl`) | Controllers and other services depend on the interface, not the implementation — `NotificationService` in particular is designed so an email/WhatsApp implementation can be added later with zero caller changes. |
| Strategy | `NotificationService` (single `InAppNotificationServiceImpl` today); `StorageService` (single `MinioStorageServiceImpl` today, swappable to AWS S3 by config only); `AIProvider` (single `OpenAICompatibleProvider` today plus a `@Profile("dev")`-gated `MockAIProvider`, resolved by name via `AIProviderResolver`); `InstagramPublisher` (`MetaGraphPublisher` — the real Meta Graph API — plus a `@Profile("dev")`-gated `MockInstagramPublisher`, resolved by name via `PublisherResolver` off `app.instagram.active-publisher`) | Same reasoning across all four: callers depend only on the interface, so Gemini/Claude/Azure/Ollama (AI) or a future Facebook/TikTok cross-poster (Instagram) can be added as further implementations with zero caller changes. `InstagramServiceImpl` never imports `MetaGraphPublisher` or `MockInstagramPublisher` directly — only `InstagramPublisher` and `PublisherResolver`. |
| Specification | `DraftSpecifications` (search/status/contentType/mediaId for `GET /api/drafts`); `ApprovalSpecifications` (status/search/date-range for `GET /api/approval/pending`) - both via `JpaSpecificationExecutor`, instead of an exploding number of derived query methods | Any endpoint needing free-text search combined with several independent optional filters plus pagination and sorting. |
| DTO | Every request/response type under `*/dto` | Entities are never serialized directly to JSON. |
| Mapper | `DraftMapper`, `ApprovalMapper`, `InstagramMapper` (entity → response DTO) | Modules 1–3 fold this into a static `Response.from(entity)` factory; Modules 4–6 use a dedicated `@Component` instead, since a curation/workflow-layer entity is more likely to need mapping logic that depends on more than just the entity itself (e.g. `ApprovalMapper` assembles the comment list alongside the approval itself). |
| Global exception handling | `GlobalExceptionHandler` (`@RestControllerAdvice`) | Centralizes HTTP status mapping for all controller-thrown exceptions; `ResourceNotFoundException`/`BadRequestException`/`ConflictException` are generic enough that Modules 2–6 reuse them as-is with no new exception types (Module 6 adds one genuinely new type, `InstagramPublisherException`, for third-party publisher errors that need their own `Reason` → HTTP status mapping). |
| Chain of Responsibility | Servlet filter chain: CORS → `JwtAuthenticationFilter` → Spring Security authorization → controller | Standard Spring Security filter chain composition. |
| Builder | Lombok `@Builder` on `User`, `RefreshToken`, `PasswordResetToken`, `Notification`, `ActivityLog`, `AppSetting`, `Media`, `PromptTemplate`, `GeneratedContent`, `GenerationHistory`, `ContentDraft`, `Approval`, `ApprovalHistory`, `ApprovalComment`, `InstagramAccount`, `InstagramPost`, `InstagramPublishHistory` | Readable, immutable-style entity construction. |
| Template/Marker interface | `AuthenticatedActor` | Lets `common` (JPA auditing, audit log) resolve "who is acting" without depending on the concrete `User`/`UserPrincipal` type in `modules/user`. |
| `REQUIRES_NEW` transaction escape hatch | `GenerationFailureRecorder` (Module 3); `PublishTransactionHelper` (Module 6) | A `FAILED` history/audit/notification record — and, in Module 6's case, the `PUBLISH_STARTED` notification fired *before* the publisher call — must survive even when the triggering `@Transactional` method re-throws and rolls back. A separate bean with `@Transactional(propagation = REQUIRES_NEW)` commits independently of the caller's transaction. Module 6 initially fired the started-notification directly inside `doPublish()`'s own transaction; manual verification caught that a failed publish silently erased it on rollback, which is why it also moved behind this same escape hatch (see `INSTAGRAM_PUBLISHER.md` §Bugs Found for the full story). |
| State machine (lightweight, service-enforced) | `Approval.status` (`PENDING_APPROVAL → APPROVED/REJECTED → READY_FOR_PUBLISH`), guarded in `ApprovalServiceImpl`/`ApprovalValidator`; `InstagramPost.status` (`PUBLISHED`/`FAILED`, one row per attempt rather than one row transitioning in place), guarded in `InstagramServiceImpl`/`InstagramValidator` — neither uses a dedicated state-machine library | The full set of transitions is small and unlikely to grow quickly enough to justify a framework; a partial unique index (`one PENDING_APPROVAL per content_id`; `one active instagram_accounts row`) backs the equivalent guarantee at the database level as defense in depth. |

## 7. Frontend Architecture

```
frontend/src/
├── app/
│   ├── layout.tsx                 Root layout: fonts, ThemeProvider, TooltipProvider
│   ├── page.tsx                   "/" → redirect("/dashboard")
│   ├── (auth)/
│   │   ├── layout.tsx             Centered, sidebar-free layout
│   │   └── login/
│   │       ├── page.tsx
│   │       └── login-form.tsx     Client component: email/password form, loading/error states
│   └── (dashboard)/
│       ├── layout.tsx             Async Server Component: fetches current user, redirects if absent
│       ├── dashboard/page.tsx     Overview page (module roadmap + live backend health check)
│       ├── {media,content,drafts,approvals,instagram}/page.tsx
│       │                           Built out (Modules 2–6)
│       └── {website,scheduler,analytics,settings,activity-log,
│           notifications}/page.tsx
│                                   Placeholder pages for every not-yet-built module
├── components/
│   ├── layout/                    Sidebar, Topbar, MobileNav, ThemeToggle, ChangePasswordDialog
│   ├── ui/                        shadcn/ui primitives (button, card, dialog, dropdown-menu, ...)
│   ├── dashboard/                 BackendStatusCard (live health-check widget)
│   ├── instagram/                 ConnectionPanel, PublishingQueue, PublishDialog, InstagramHistoryList (Module 6)
│   └── shared/                    ModulePlaceholder (reused by every "coming soon" page)
├── lib/
│   ├── api/                       client.ts (fetch wrapper), auth.ts, health.ts, server-auth.ts, ..., instagram.ts
│   └── nav-config.ts               Single source of truth for sidebar navigation
├── types/{user,...,instagram}.ts  Shared frontend types per module
└── proxy.ts                       Route guard + silent token refresh (see §9)
```

**Note on `proxy.ts`:** this project runs Next.js 16, which renamed the `middleware.ts` convention to `proxy.ts` (same execution model: it runs ahead of every matched request). This is a real, intentional Next.js 16 API change, not a naming mistake.

Session data flow: the `(dashboard)` layout is an `async` Server Component. It calls `getCurrentUserServerSide()`, which forwards the incoming request's `Cookie` header to `GET /api/auth/me` on the backend using a server-only base URL (`API_INTERNAL_BASE_URL`, distinct from the browser-facing `NEXT_PUBLIC_API_BASE_URL` — see §11). If the backend returns anything other than a successful user payload, the layout calls `redirect("/login")` before rendering anything. The resolved `user` object is passed as a prop into the client-side `Topbar`, which is the only place session data reaches client-side React state.

## 8. Database Architecture

- **Engine:** PostgreSQL 16.
- **Migration tool:** Flyway, `spring.jpa.hibernate.ddl-auto: validate` — Hibernate is only ever allowed to *validate* that entity mappings match the schema Flyway created; it can never generate or alter DDL itself.
- **Primary keys:** every table uses a `UUID` primary key generated **application-side** by Hibernate (`@UuidGenerator` on `BaseEntity`), not a database default — so migrations never depend on `pgcrypto`/`uuid-ossp`.
- **Migrations applied so far:** `V1__init_schema.sql` (Module 0: `activity_log`, `notifications`, `app_settings`), `V2__auth_and_users.sql` (Module 1: `users`, `refresh_tokens`, `password_reset_tokens`), `V3__media.sql` (Module 2: `media`), `V4__ai_content_generation.sql` (Module 3: `prompt_templates`, `generated_content`, `generation_history`), `V5__prompt_template_seed.sql` (Module 3: seeds the 17 default templates), `V6__content_drafts.sql` (Module 4: `content_drafts`), `V7__approval_workflow.sql` (Module 5: `approvals`, `approval_history`, `approval_comments`), and `V8__instagram_publisher.sql` (Module 6: `instagram_accounts`, `instagram_posts`, `instagram_publish_history`). Full detail in `DATABASE_DESIGN.md`.

## 9. Security Architecture

### Authentication flow (narrative)

1. Client submits credentials to `POST /api/auth/login`.
2. `LoginRateLimiter` (Bucket4j, 5 attempts/minute per client IP, in-memory) is checked first; over the limit returns `429`.
3. Spring's `AuthenticationManager` authenticates against `CustomUserDetailsService`, which loads the `User` by email and wraps it in `UserPrincipal` (implements both `UserDetails` and the shared `AuthenticatedActor` marker).
4. On success: `user.lastLogin` is updated, an access token (JWT) and a refresh token (opaque, DB-backed) are issued, both set as **HttpOnly** cookies, an `ActivityAction.LOGIN` audit entry is written, and a `LOGIN_SUCCESS` in-app notification is created.
5. On failure: an `ActivityAction.LOGIN_FAILED` audit entry is written (email + client IP, no password) and a generic `401 Invalid email or password` is returned — the same message for "unknown email" and "wrong password" so the API never discloses which one it was.

### JWT flow

- **Signing:** HMAC-SHA256 via `io.jsonwebtoken` (JJWT). The configured `app.jwt.secret` string is used directly as the raw HMAC key bytes (UTF-8), so it must be at least 32 characters — it is not required to be base64.
- **Claims:** `sub` = user UUID, `email`, `role`, `iat`, `exp`.
- **Lifetime:** 15 minutes by default (`app.jwt.access-token-ttl-minutes`).
- **Verification:** `JwtAuthenticationFilter` (a `OncePerRequestFilter` registered ahead of `UsernamePasswordAuthenticationFilter`) extracts the token from the `access_token` cookie (falling back to an `Authorization: Bearer` header if no cookie is present), parses and verifies the signature/expiry, then **reloads the user from the database by email** on every request (via `CustomUserDetailsService`) rather than trusting the token's claims for authorization state — this means a deactivated/suspended account loses access immediately, not just after its token expires.

### Refresh token flow

- Refresh tokens are **not** JWTs. They are 256-bit random values (`SecureRandom`, base64url-encoded) generated by `SecureTokenUtil`. Only the **SHA-256 hash** of the token is ever persisted (`refresh_tokens.token_hash`); the raw value exists only in the HttpOnly cookie and the brief in-memory response.
- **Rotation:** every call to `POST /api/auth/refresh` revokes the presented token and issues a brand-new one (`RefreshTokenServiceImpl.rotate`). The old row is marked `revoked = true`, `revoked_at` is set, and `replaced_by_token_hash` links to the new row for traceability.
- **Reuse/theft detection:** if a token that is *already revoked* is presented again, the system treats this as evidence of token theft/replay and immediately revokes **every** active refresh token for that user, forcing a fresh login everywhere.
- **Invalidation triggers:** logout revokes the single presented token; password change (and password reset) revoke **all** refresh tokens for the user, then issue one fresh pair so the current session stays logged in while every other session is killed.
- **Cookie scope:** the refresh token cookie's `Path` is restricted to `/api/auth`, so it is never sent on ordinary API calls — only auth endpoints ever see it.

### Cookie configuration

All cookies are `HttpOnly` always. `Secure` and `SameSite` are configurable (`app.cookie.secure`, `app.cookie.same-site`), defaulting to `Secure=true`/`SameSite=Lax` in the base config and overridden to `Secure=false` in the `dev` profile (plain HTTP over `localhost`). `CookieProperties` also supports an optional shared `domain` for a future split-subdomain deployment.

### CSRF posture

CSRF protection is disabled (`http.csrf(disable)`) because the API is stateless and cookie-based CSRF is mitigated by two other controls working together: (1) `SameSite=Lax` on both auth cookies, and (2) CORS configured with an **explicit allow-list of origins** (`app.cors.allowed-origins`) plus `allowCredentials(true)` — never a wildcard. A browser cannot get a cross-site request's cookies honored by a non-listed origin, and simple cross-site requests without CORS pre-flight still won't have the cookie sent cross-site under `SameSite=Lax` for state-changing verbs.

### Authorization

- `@EnableMethodSecurity` + `@PreAuthorize` for role checks. `UserController` is annotated at the class level with `@PreAuthorize("hasRole('ADMIN')")` — every `/api/users/**` endpoint requires the `ADMIN` role. `AuthController`'s protected endpoints use `@PreAuthorize("isAuthenticated()")`.
- Unauthenticated/forbidden requests get a consistent `ApiResponse` JSON body via `RestAuthenticationEntryPoint` (401) and `RestAccessDeniedHandler` (403) — these run at the Spring Security filter-chain level, which is why they exist separately from `GlobalExceptionHandler` (the filter chain rejects requests before they ever reach a controller, so `@ExceptionHandler` methods never see them).

### Password handling

- `BCryptPasswordEncoder` (Spring Security default strength). Passwords are never logged, never returned in any response DTO, and `User.toString()` explicitly excludes the `password` field (`@ToString(exclude = "password")`).
- Password strength is enforced via a shared regex (`ValidationPatterns.PASSWORD_STRENGTH`): minimum 8 characters, at least one letter and one digit.

### Third-party credential encryption (Module 6)

`CredentialEncryptionUtil` (`common/util`) encrypts Instagram access tokens at rest with **AES-256-GCM** (authenticated encryption, not plain AES-CBC) — distinct from `SecureTokenUtil`'s one-way SHA-256 hashing, since a refresh/reset token only ever needs to be *compared*, while a third-party access token must be *decrypted* again to make Graph API calls on the academy's behalf. A random 96-bit IV is generated per encryption call (`SecureRandom`) and stored alongside the ciphertext (`base64(iv || ciphertext)`) — GCM's authentication tag means any tampering with the stored value fails decryption loudly rather than silently returning corrupted plaintext. The key itself (`app.security.encryption-key`, base64-encoded 32 bytes) is env-injected the same way `app.jwt.secret` is; `CredentialEncryptionUtil`'s constructor fails application startup immediately if the key is missing or not exactly 32 bytes, rather than deferring the failure to the first encrypt/decrypt call. `InstagramAccountResponse` never includes the token field at all (not even encrypted) — the encrypted value exists only in the database and in `InstagramServiceImpl`'s brief in-memory decryption right before a publish call.

## 10. Docker Architecture

`infra/docker-compose.yml` defines four services:

| Service | Image / Build | Port(s) | Depends on |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `${POSTGRES_HOST_PORT:-5433}:5432` | — |
| `minio` | `minio/minio:latest` | `9000` (API), `9001` (console) | — |
| `backend` | multi-stage build from `backend/Dockerfile` | `8080:8080` | `postgres` (healthy), `minio` (started) |
| `frontend` | multi-stage build from `frontend/Dockerfile` | `3000:3000` | `backend` |

The Postgres host port defaults to `5433`, not `5432` — this avoids colliding with any other Postgres container a developer might already have running locally. Backend and frontend read all configuration from environment variables (see `.env.example`), with the frontend receiving **two** API base URLs: `NEXT_PUBLIC_API_BASE_URL` (browser-facing, `http://localhost:8080/api`) and `API_INTERNAL_BASE_URL` (server-side-only, `http://backend:8080/api` inside Compose) — the frontend container's own `localhost` is not the backend, so a single shared URL would break either the browser or the server-side `proxy.ts`/Server Component calls.

## 11. MinIO Integration (current status)

MinIO runs as a Docker service and the `io.minio:minio` Java client is a backend dependency, but **there is no `StorageService`, `MinioConfig`, upload endpoint, or bucket-provisioning code in the repository yet**. This is intentional: MinIO is provisioned now so the infrastructure is ready, and will be wired up when the Media Upload module is built. Treat any mention of file/media storage beyond "the container exists" as not yet implemented.

## 12. Flyway Migration Strategy

- One migration file per logical change, named `V<n>__description.sql`, applied in order and checksummed by Flyway on every boot (`spring.flyway.enabled: true`, `baseline-on-migrate: true`).
- `ddl-auto: validate` means a mismatch between entity mappings and the actual schema fails application startup loudly instead of silently drifting.
- All foreign keys use `ON DELETE CASCADE` where the child row is meaningless without its parent (`refresh_tokens.user_id`, `password_reset_tokens.user_id`).
- Indexes are added deliberately for every column used in a `WHERE`/`JOIN` by a repository method (e.g. `idx_users_email`, `idx_refresh_tokens_hash`), not generated automatically.

## 13. API Design Principles

- Every response body — success or failure — is wrapped in `ApiResponse<T>` (`success`, `message`, `data`, `errors`, `timestamp`). Paginated list endpoints wrap the page in `PageResponse<T>` inside `ApiResponse.data`.
- Bean Validation (`jakarta.validation`) on every request DTO; validation failures return `400` with a per-field message list, handled centrally by `GlobalExceptionHandler`.
- Authentication state travels exclusively via HttpOnly cookies for browser clients; an `Authorization: Bearer` header is also accepted for tooling/non-browser clients.
- REST resource naming follows plural nouns (`/api/users`) with the auth surface grouped under `/api/auth/*` as actions rather than resources, since login/logout/refresh are operations, not CRUD on a resource.

## 14. Folder Structure Explanation

See §6 and §7 above for the annotated backend/frontend trees. The guiding rule: **package by feature, not by layer, above the `common`/`config`/`security` cross-cutting packages.** A new module (e.g. the future Media Upload module) gets its own `modules/media/` package containing its entity, repository, service, controller, and DTOs together — it does not scatter its controller into a shared `controllers/` package.

## 15. Future Module Integration Strategy

Modules not yet built (website publishing, scheduler, analytics, activity log viewer, notification center UI, settings UI) are expected to plug into infrastructure already in place rather than rebuild it. Modules 2–6 confirmed this works in practice, not just in theory:

- **Audit trail**: call `AuditLogService.record(...)` — used by every module since Auth with zero changes to the interface. Module 4 needed one new `ActivityAction.RESTORE` constant; Module 5 needed `SUBMIT`/`COMMENT`; Module 6 needed `CONNECT`/`DISCONNECT`/`RETRY` (all additive enum extensions, never a signature change — Module 6 also reuses the pre-existing `PUBLISH` constant for a first-time publish).
- **Notifications**: call `NotificationService.notify(...)` — same interface since Module 1. Modules 3, 4, 5, and 6 each added their own `NotificationType` constants (`GENERATION_COMPLETED`/`GENERATION_FAILED`; `DRAFT_SAVED`/`DRAFT_UPDATED`/`DRAFT_DELETED`/`DRAFT_RESTORED`; `APPROVAL_COMMENT_ADDED`, plus reusing `APPROVAL_REQUIRED`/`APPROVED`/`REJECTED` that had sat unused in the enum since Module 0; `PUBLISH_STARTED`, plus reusing `PUBLISH_SUCCESS`/`PUBLISH_FAILURE` that had likewise sat unused since Module 0) without touching the service itself.
- **Authorization**: `@PreAuthorize("hasRole('ADMIN')")` (class-level) is the established pattern for admin-only modules (Media, AI, Prompts, Drafts, Approvals, Instagram all use it identically); new roles are added by extending the `Role` enum, no filter-chain changes required.
- **Settings**: the `AppSetting` entity/repository scaffold from Module 0 is still awaiting its full CRUD service + controller.
- **Storage**: the MinIO container and client dependency, provisioned in Module 0, are used as-is by Module 2's `StorageService` — and, in turn, by Module 6's `InstagramServiceImpl` (`storageService.presignedGetUrl(...)` to hand the Graph API a browser-reachable image URL for the media container it creates).
- **Navigation**: every future page already has a placeholder route and a `lucide-react` icon registered in `frontend/src/lib/nav-config.ts` — building a module means replacing the placeholder page and flipping its `status` to `"available"`, not adding new routing/shell code. Instagram made this exact flip for Module 6.
- **Cross-module references without hard coupling**: Module 4's `ContentDraft.generatedContentId`/`mediaId`, Module 5's `Approval.contentId`, and Module 6's `InstagramPost.approvalId`/`instagramAccountId`/`mediaId` all follow the same pattern Module 3 established for `GeneratedContent.mediaId` — a plain nullable UUID column with `ON DELETE SET NULL`, not a JPA `@ManyToOne`. Deleting a `Media`/`GeneratedContent`/`ContentDraft`/`Approval` row never fails or cascades unexpectedly; dependents just lose the back-reference and keep their own denormalized copy of whatever they needed to display (`ContentDraft` keeps its own `contentType` and text snapshot; `Approval` keeps its own `contentTitle`/`contentType` snapshot; `InstagramPost` keeps its own `caption`/`hashtags` rather than re-reading them from the approval at display time).
- **Reaching into another module's repository, not its service, for a narrow need**: `ApprovalServiceImpl` injects `DraftRepository` directly (to read/update a draft's status) rather than going through `DraftService`, specifically to avoid inheriting Module 4's own audit/notification side effects on top of Module 5's own. `InstagramServiceImpl` does the same with `ApprovalRepository` (read-only — checking `Approval.status`/`contentTitle`) and `MediaRepository` (read-only — resolving the selected media row), while still going through the real `StorageService` *service* (not a raw MinIO repository) since presigned-URL generation is genuine business logic, not a plain CRUD read. The same reasoning applies in reverse to `UserRepository` (injected by Module 5 to look up active admins to notify on submission) — a repository is a safe, narrow, read/write dependency; a service carries its whole module's cross-cutting behavior with it.
- **Strategy-pattern provider modules follow one shape**: Module 6's `provider/` package (`InstagramPublisher` interface, `MetaGraphPublisher` real impl, `MockInstagramPublisher` `@Profile("dev")` impl, `PublisherResolver` name-based resolver bean) is structurally identical to Module 3's `modules/ai/provider/` (`AIProvider`, `OpenAICompatibleProvider`, `MockAIProvider`, `AIProviderResolver`) — the same shape was reused rather than redesigned, down to both mocks supporting a `SIMULATE_FAILURE` caption/prompt marker for exercising the failure path in `dev`.
