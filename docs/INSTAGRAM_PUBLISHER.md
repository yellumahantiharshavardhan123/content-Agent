# Instagram Publisher — Module 6

**Status:** Complete and verified. Production Meta credentials are not yet available, so the system currently runs against a `dev`-only mock publisher — see §6 for exactly what changes (config only, no code) once real credentials arrive.

This document is the single reference for Module 6. `API_DOCUMENTATION.md` has the full request/response contracts, `DATABASE_DESIGN.md` has the full column-by-column schema, and `ARCHITECTURE.md`/`SYSTEM_ARCHITECTURE.md` have the pattern/sequence-diagram detail — this document ties those together into one narrative and covers the operational questions (how to switch publishers, how to go live) the others don't.

---

## 1. What this module does

Takes an `Approval` (Module 5) that has reached `READY_FOR_PUBLISH` and publishes it to a connected Instagram Business Account: select the media, write/edit the final caption and hashtags, preview, publish, and see the result in a running history feed — with an audit trail and notifications at every step.

## 2. The core design decision: publishers are swappable by configuration alone

The academy does not yet have production Meta credentials. Rather than stub the feature out or hardcode a fake response, the entire real integration was built — the real two-step Meta Graph API publish flow, real credential storage, real error mapping — and a second, `dev`-only implementation was added beside it. Which one runs is a single config value:

```
app.instagram.active-publisher: meta-graph-api    # or: mock
```

```
                    ┌─────────────────────────┐
                    │   InstagramPublisher      │  ◀── interface (provider package)
                    │  - getPublisherName()      │
                    │  - verifyAccount(id, token)│
                    │  - publish(request)        │
                    └────────────┬────────────┘
                                 │ implements
              ┌──────────────────┴──────────────────┐
              ▼                                     ▼
  ┌─────────────────────────┐      ┌─────────────────────────────┐
  │     MetaGraphPublisher    │      │     MockInstagramPublisher    │
  │  "meta-graph-api"          │      │  "mock"                        │
  │  real HTTPS calls to       │      │  @Profile("dev") only -        │
  │  graph.facebook.com        │      │  doesn't exist as a bean        │
  │  (RestClient)               │      │  outside dev, even              │
  │                             │      │  misconfigured                  │
  └─────────────────────────┘      └─────────────────────────────┘
              ▲                                     ▲
              └──────────────────┬──────────────────┘
                                 │ selects by name
                    ┌─────────────────────────┐
                    │     PublisherResolver      │
                    │  resolve() -> active        │
                    │  resolveMock() -> always     │
                    │  the mock (explicit          │
                    │  /publish/mock endpoint)     │
                    └─────────────────────────┘
                                 ▲
                                 │ injected
                    ┌─────────────────────────┐
                    │   InstagramServiceImpl    │
                    │  never imports              │
                    │  MetaGraphPublisher or       │
                    │  MockInstagramPublisher —    │
                    │  only the interface + resolver│
                    └─────────────────────────┘
```

This is the exact shape Module 3 established for `AIProvider`/`OpenAICompatibleProvider`/`MockAIProvider`/`AIProviderResolver` — reused deliberately rather than redesigned. `InstagramServiceImpl` has zero knowledge of which publisher is active; it calls `publisherResolver.resolve()` and programs against the interface. Swapping publishers, or adding a third one later (a Facebook or TikTok cross-poster, for instance), never touches `InstagramServiceImpl`, `InstagramController`, or any DTO.

## 3. Configuration reference

| Variable | Applies to | Purpose |
|---|---|---|
| `IG_ACTIVE_PUBLISHER` → `app.instagram.active-publisher` | both | `meta-graph-api` (real) or `mock`. **The `dev` Spring profile hardcodes this to `mock` in `application-dev.yml` regardless of this value** — a `dev`-profile deployment cannot accidentally call the real Graph API. `prod`/base config defaults to `meta-graph-api`. |
| `IG_GRAPH_API_BASE_URL` → `app.instagram.graph-api-base-url` | real only | Defaults to `https://graph.facebook.com/v19.0`. |
| `IG_APP_ID` / `IG_APP_SECRET` → `app.instagram.app-id`/`app-secret` | real only | Reserved for a future OAuth/token-refresh flow; not read by any code path yet — connecting today takes a long-lived access token directly via `POST /api/instagram/connect`. |
| `IG_BUSINESS_ACCOUNT_ID` / `IG_ACCESS_TOKEN` → `app.instagram.business-account-id`/`access-token` | optional, both | If both are set **and** no account is currently connected, `InstagramAccountBootstrapRunner` auto-connects on startup — convenient for a fixed deployment where the account never changes. Leave blank to connect manually via the UI/API instead. |
| `APP_ENCRYPTION_KEY` → `app.security.encryption-key` | both | Base64-encoded 256-bit AES key used to encrypt the stored access token. **Required at startup** — `CredentialEncryptionUtil`'s constructor throws immediately if it's missing or not exactly 32 bytes, so a misconfigured deployment fails fast at boot rather than failing confusingly on first connect. Generate with `openssl rand -base64 32`. |

## 4. Publish workflow (end to end)

1. **Select**: the Publishing Queue lists every `Approval` at `READY_FOR_PUBLISH` (`GET /api/approval/pending?status=READY_FOR_PUBLISH`, the same Module 5 endpoint the Approval Dashboard uses).
2. **Compose**: the Publish dialog loads the source draft's text as a starting caption (`GET /api/drafts/{contentId}`) and the media library for image selection; both are editable before publishing.
3. **Preview**: the dialog renders exactly what will be posted — caption + hashtags concatenated the same way the backend does (`caption + "\n\n" + hashtags`).
4. **Publish**: `POST /api/instagram/publish` (or `/publish/mock` to force the mock regardless of the active-publisher config — useful for a safe dry run even when a real account is connected).
5. **Backend**: loads the active account, validates the approval is `READY_FOR_PUBLISH` and not already published, fires the `PUBLISH_STARTED` notification, resolves a presigned URL for the selected media, decrypts the stored access token, and calls the resolved publisher.
6. **Real publisher** (`MetaGraphPublisher`): `POST /{ig-user-id}/media` (create a container from the image URL + caption) → `POST /{ig-user-id}/media_publish` (publish the container) → best-effort `GET /{media-id}?fields=permalink` (a failure here does not fail the publish — the post is already live).
7. **Persist**: only *after* the publisher call resolves does anything get written — a successful `InstagramPost` row (`status: PUBLISHED`) or, on failure, a `FAILED` row recorded independently (see §5).
8. **Result**: `PUBLISH_SUCCESS`/`PUBLISH_FAILURE` notification, `activity_log` entry (`PUBLISH` for a first attempt, `RETRY` if a prior `FAILED` row exists for the same approval), and a new `instagram_publish_history` entry either way.

## 5. Why nothing is written before the external call

`doPublish()` deliberately does not insert an `InstagramPost` row and then update it — it builds the request in memory, calls the publisher, and only persists once the outcome is known. This mirrors `GenerationFailureRecorder` from Module 3 and matters because of ordinary `@Transactional` rollback semantics: if a row were inserted first and the publisher call then threw, the whole method's transaction would roll back and silently erase the very row meant to explain what happened.

On failure, `PublishTransactionHelper.recordFailure(...)` runs in its **own** `@Transactional(propagation = REQUIRES_NEW)` transaction — independent of the caller's, so it survives the exception that's re-thrown immediately after it returns. It persists the `FAILED` post row, a `PUBLISH_FAILURE` history entry, the audit log entry, and the failure notification, all four together or not at all.

## 6. Going live with real Meta credentials

Per the explicit requirement this module was built against, **no backend or frontend code changes** are needed — only configuration:

1. Create a Meta Business App, complete Instagram Graph API review, obtain a long-lived Page access token for the academy's connected Instagram Business Account.
2. Set `IG_ACTIVE_PUBLISHER=meta-graph-api` (already the default outside the `dev` profile — this step matters only if it was ever overridden).
3. Either set `IG_BUSINESS_ACCOUNT_ID`/`IG_ACCESS_TOKEN` for auto-connect on the next boot, or call `POST /api/instagram/connect` once from the running admin UI.
4. Confirm `APP_ENCRYPTION_KEY` is set to a real, durably-stored key in that environment (if it ever changes after tokens are stored, every stored token becomes undecryptable — reconnect afterward).

`MetaGraphPublisher` already implements the full real flow (§4, steps 6) and its full error mapping (§7) — it has simply never been exercised against production Meta infrastructure yet, since no credentials exist to do so.

## 7. Error handling

`InstagramPublisherException.Reason` classifies every failure from either publisher and `GlobalExceptionHandler` maps it to an HTTP status, the same pattern Module 3 uses for `AIProviderException`:

| Reason | HTTP | Meaning |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Meta rejected the access token (connect or publish time) |
| `INVALID_MEDIA` | 422 | Meta rejected the image itself |
| `RATE_LIMITED` | 429 | Meta Graph API rate limit hit |
| `TIMEOUT` | 504 | Request to Meta timed out |
| `UNAVAILABLE` / `UNKNOWN` | 503 | Meta unreachable, or an unrecognized response shape |

## 8. Security

- **Encryption at rest**: access tokens are AES-256-GCM encrypted (`CredentialEncryptionUtil`) before being stored in `instagram_accounts.access_token_encrypted`. See `ARCHITECTURE.md` §9 for the full rationale (why GCM, not just AES-CBC; why a dedicated utility distinct from the SHA-256 one-way hashing `SecureTokenUtil` already uses for refresh/reset tokens).
- **Never returned**: no response DTO (`InstagramAccountResponse`, `InstagramPostResponse`, `InstagramHistoryResponse`) includes the token field, encrypted or otherwise. Verified directly via `psql` during manual verification — the stored column holds real ciphertext, and no API response, in any code path, includes it.
- **ADMIN only**: `InstagramController` is class-level `@PreAuthorize("hasRole('ADMIN')")`, identical to every other admin-surface controller since Module 3.
- **One active connection at a time**: enforced at the service layer (`InstagramValidator.validateNoActiveConnection`) and at the database level (`idx_instagram_accounts_one_active`, a partial unique index) as defense in depth.

## 9. Development & testing

- **Mock publisher** (`MockInstagramPublisher`, `@Profile("dev")`): simulates realistic latency (300–900ms), returns a synthetic `instagram_media_id`/`permalink`, and never makes a network call. It does not exist as a Spring bean at all outside the `dev` profile — a production misconfiguration cannot accidentally select it.
- **Failure simulation**: include the literal string `SIMULATE_FAILURE` anywhere in the caption to make the mock publisher throw (`Reason.UNAVAILABLE`) — this is how the failure/retry path was exercised end-to-end without needing the real Graph API to be down.
- **`/api/instagram/publish/mock`**: always uses the mock regardless of `app.instagram.active-publisher`, so a real connected account can still be safely dry-run tested.
- **Automated tests** (33 new, all passing — see `IMPLEMENTATION_REPORT.md` §11 for the full-suite count): `CredentialEncryptionUtilTest` (6 — round-trip, tampered-ciphertext rejection, wrong-key rejection, invalid-key-length startup failure), `MockInstagramPublisherTest` (4 — verify/publish success, `SIMULATE_FAILURE` trigger, distinct media ids per call), `InstagramServiceImplTest` (11 — connect/status/publish/disconnect success and error paths, including the already-published/duplicate-connection conflicts and the retry-detection logic), `InstagramControllerIntegrationTest` (12 — full HTTP-level flow against a real Testcontainers Postgres, including the encrypted-token-never-exposed assertion and the global history feed).

## 10. Manual verification performed

Beyond the automated suite, the full flow was exercised against the real running Docker stack (mock publisher active, per the `dev` profile default):

- `GET /status` → `connected: false` before any connection exists.
- `POST /connect` → `connected: true`, response contains no token field; direct `psql` query confirmed `access_token_encrypted` holds ciphertext, not the plaintext token supplied.
- `POST /publish` (success path) → `PUBLISHED`, `instagramMediaId`/`permalink` populated.
- `POST /publish/mock` (explicit mock endpoint) → identical success shape.
- `POST /publish` with `SIMULATE_FAILURE` in the caption → `503`, `FAILED` post row persisted with the error message.
- Retry of the same approval with a corrected caption → succeeds, `activity_log` records `RETRY` (not `PUBLISH`).
- Repeat publish of an already-`PUBLISHED` approval → `409 Conflict`, no duplicate post created.
- `DELETE /disconnect` → `connected: false`; a subsequent publish attempt → `409` ("no account connected"); reconnect succeeds.
- Direct DB inspection confirmed: `instagram_posts` (3 `PUBLISHED` + 1 `FAILED` from the exercises above), `instagram_publish_history` (one row per action, `CONNECT`/`PUBLISH_SUCCESS`×3/`PUBLISH_FAILURE`/`DISCONNECT`/`CONNECT`), `activity_log` (`CONNECT`, `PUBLISH`×3, `RETRY`, `DISCONNECT`), and `notifications` (`PUBLISH_STARTED`/`PUBLISH_SUCCESS`/`PUBLISH_FAILURE` in the expected counts — see §11 for the one bug this last check caught).
- All test fixtures and the rows they produced were cleaned up afterward (scoped deletes by explicit id/title, not a table-wide wipe — audit-trail rows in `activity_log` and the `notifications` inbox were deliberately left in place rather than deleted, consistent with those tables being an intentional record of real actions that occurred, not disposable scratch data).

## 11. Bugs found and fixed during this module

### `PUBLISH_STARTED` notification silently lost on a failed publish

- **Symptom**: found via a deliberate before/after count check on the `notifications` table around a `SIMULATE_FAILURE` publish attempt — the count of `PUBLISH_STARTED` rows did not increase, even though `doPublish()` unconditionally calls `notificationService.notify(..., PUBLISH_STARTED, ...)` near the top of the method, before the publisher is ever called.
- **Root cause**: that notification call ran inside `doPublish()`'s own `@Transactional` method. When the publisher call failed and the method's `catch` block re-threw the exception, Spring's default rollback-on-unchecked-exception behavior rolled back the *entire* transaction — including the `PUBLISH_STARTED` insert that had logically already "happened" earlier in the same method body. The failure path ended up with a `PUBLISH_FAILURE` notification but no paired `PUBLISH_STARTED` one, while the success path had both — an inconsistency directly contradicting the explicit requirement that all three notification types ("Publishing Started/Successful/Failed") fire reliably.
- **Fix**: extracted the started-notification into `PublishTransactionHelper.notifyStarted(...)`, a second `@Transactional(propagation = REQUIRES_NEW)` method alongside the pre-existing `recordFailure(...)` on the same bean (renamed from `PublishFailureRecorder` to `PublishTransactionHelper` to honestly reflect that it now serves two related "must survive the caller's rollback" responsibilities, not just failure recording). `doPublish()` now calls `publishTransactionHelper.notifyStarted(...)` instead of the notification service directly, so the insert commits immediately and independently, regardless of what happens afterward in the same request.
- **Verification**: a precise before/after count on `notifications WHERE type = 'PUBLISH_STARTED'` around a `SIMULATE_FAILURE` publish attempt went from 3 → 4 after the fix (previously it would have stayed at 3). Two unit tests were added/updated in `InstagramServiceImplTest` to lock this in as a regression test: `publish_success_savesPublishedPostAndNotifies` now asserts `notifyStarted(...)` was called via the helper (not directly on `notificationService`), and `publish_providerFailure_recordsFailureAndRethrows` now additionally asserts `notifyStarted(...)` was called even though the attempt fails.

No other bugs were found during this module's implementation or verification.

## 12. Known limitations

- **Publishing Queue does not itself filter out approvals already published to Instagram in a previous session.** `GET /api/approval/pending?status=READY_FOR_PUBLISH` (Module 5's own endpoint, reused as-is) returns every `READY_FOR_PUBLISH` approval regardless of whether Module 6 has already published it — by design, since Module 5's status is meant to mean "ready for any downstream publisher," and a future Website Publisher (Module 8) module will need the same approval to still show up in its own queue after an Instagram-only publish. Within a single browser session, a just-published item disappears from the queue immediately (client-side state update); after a full page reload, an already-published item can reappear in the list. This is purely cosmetic — attempting to publish it again is safely rejected server-side with `409 Conflict` ("This content has already been published to Instagram"), so no duplicate publish or data corruption can occur. Resolving the queue's visual staleness would require either a new cross-module read (Module 6 exposing "already published" approval ids for Module 5's queue to filter against) or a product decision about whether "published to Instagram" should affect `Approval.status` globally — deferred rather than guessed at.
- **`IG_APP_ID`/`IG_APP_SECRET` are not yet read by any code path.** They're reserved configuration for a future OAuth/token-refresh flow (today, connecting takes a long-lived access token directly, entered once via `POST /api/instagram/connect`).
- **The real `MetaGraphPublisher` implementation has never been exercised against production Meta infrastructure**, since no real credentials exist yet (see §6). Its logic (two-step publish, error-reason classification) is fully implemented and unit-testable, but only the mock path has actually been run end-to-end.
