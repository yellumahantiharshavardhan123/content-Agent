# System Architecture Diagrams — Arjun Sports AI Content Agent

All diagrams below reflect Module 0 + Module 1 as implemented. Where a component is provisioned but not yet functionally wired up (MinIO), the diagram says so explicitly.

---

## 1. User → Frontend → Backend → PostgreSQL

```
┌──────────┐     HTTPS/HTTP      ┌───────────────────┐      REST/JSON       ┌──────────────────┐      JDBC       ┌──────────────┐
│  Admin   │ ──────────────────▶ │  Next.js Frontend  │ ───────────────────▶ │  Spring Boot API  │ ───────────────▶ │  PostgreSQL  │
│ (Browser)│ ◀────────────────── │      :3000         │ ◀─────────────────── │      :8080        │ ◀─────────────── │     16       │
└──────────┘   HTML + cookies    └───────────────────┘   ApiResponse<T>      └──────────────────┘   Hibernate/JPA  └──────────────┘
```

**Explanation:** the browser only ever talks to the Next.js server (port 3000). Next.js's server-side code (Server Components, `proxy.ts`) and the browser's own `fetch` calls both go on to the Spring Boot API (port 8080). Spring Data JPA/Hibernate is the only component that talks to PostgreSQL; the frontend has no database credentials or driver at all. Every row read from or written to PostgreSQL passes through a `@Repository` interface, then a `@Service`, then is shaped into a DTO before it ever becomes JSON.

## 2. User → Frontend → Backend → MinIO

```
┌──────────┐                 ┌───────────────────┐                 ┌──────────────────┐          ┌──────────────┐
│  Admin   │ ── (not yet) ──▶ │  Next.js Frontend  │ ── (not yet) ─▶ │  Spring Boot API  │ ── X ──▶ │    MinIO     │
│ (Browser)│                 │      :3000         │                 │      :8080        │          │  :9000/:9001 │
└──────────┘                 └───────────────────┘                 └──────────────────┘          └──────────────┘
                                                                      no StorageService
                                                                      or upload endpoint
                                                                      exists yet
```

**Explanation:** MinIO runs as a container in `docker-compose.yml` and the `io.minio:minio` client library is a backend dependency, but **no code path currently connects to it** — there is no `StorageService`, no MinIO `@Configuration` class, and no upload controller. This diagram intentionally shows the gap: MinIO is infrastructure-ready for the future Media Upload module, not yet integrated.

## 3. JWT Authentication Flow (token structure and verification)

```
                              ACCESS TOKEN (JWT, HS256, 15 min TTL)
        ┌─────────────────────────────────────────────────────────────────────┐
        │  header.payload.signature                                          │
        │  payload = { sub: <userId>, email, role, iat, exp }                │
        │  signature = HMAC_SHA256(header + "." + payload, app.jwt.secret)   │
        └─────────────────────────────────────────────────────────────────────┘
                                       │
                                       │ sent as HttpOnly cookie "access_token"
                                       ▼
        ┌─────────────────────────────────────────────────────────────────────┐
        │  JwtAuthenticationFilter (every request)                            │
        │  1. Read "access_token" cookie (fallback: Authorization: Bearer)    │
        │  2. Verify signature + expiry (JwtTokenProvider.parseClaims)        │
        │  3. Re-load the user from the DB by email (CustomUserDetailsService)│
        │  4. If enabled + not locked → set SecurityContext authentication   │
        └─────────────────────────────────────────────────────────────────────┘
```

**Explanation:** the access token is stateless and self-contained (its claims are trusted once the signature verifies), but the filter deliberately re-fetches the user from PostgreSQL on every request instead of trusting the token's `role`/`email` claims for authorization decisions. This is a conscious trade-off: it costs one extra query per request but means suspending or deleting a user takes effect on their very next request, not up to 15 minutes later when their token would otherwise expire.

## 4. Login Sequence

```
Browser                 Frontend (Next.js)          Backend (Spring Boot)              PostgreSQL
   │                           │                              │                              │
   │  POST /login form         │                              │                              │
   ├──────────────────────────▶│                              │                              │
   │                           │  POST /api/auth/login        │                              │
   │                           │  {email, password}           │                              │
   │                           ├─────────────────────────────▶│                              │
   │                           │                              │  check rate limiter (per IP) │
   │                           │                              │  AuthenticationManager        │
   │                           │                              │  .authenticate(...)           │
   │                           │                              ├─────────────────────────────▶│
   │                           │                              │  SELECT * FROM users          │
   │                           │                              │  WHERE email = ?              │
   │                           │                              │◀─────────────────────────────┤
   │                           │                              │  BCrypt.matches(password)     │
   │                           │                              │  (success path)                │
   │                           │                              │  UPDATE users SET last_login   │
   │                           │                              ├─────────────────────────────▶│
   │                           │                              │  INSERT INTO refresh_tokens    │
   │                           │                              ├─────────────────────────────▶│
   │                           │                              │  INSERT INTO activity_log      │
   │                           │                              │  (action = LOGIN)              │
   │                           │                              ├─────────────────────────────▶│
   │                           │                              │  INSERT INTO notifications     │
   │                           │                              │  (type = LOGIN_SUCCESS)        │
   │                           │                              ├─────────────────────────────▶│
   │                           │  200 OK + Set-Cookie:         │                              │
   │                           │  access_token, refresh_token  │                              │
   │                           │◀─────────────────────────────┤                              │
   │  redirect to /dashboard   │                              │                              │
   │◀──────────────────────────┤                              │                              │
```

**Explanation:** the login form posts directly to the backend (browser `fetch` with `credentials: "include"`), not through a Next.js API route. On failure, the same rate-limit check and audit step run, but instead of updating `last_login`/writing cookies, the backend writes a `LOGIN_FAILED` activity log entry and returns `401` with a generic message that does not reveal whether the email exists.

## 5. Refresh Token Sequence

```
Next.js proxy.ts (or browser)              Backend                          PostgreSQL
        │                                      │                                 │
        │  access_token cookie missing/expired,│                                 │
        │  refresh_token cookie present         │                                 │
        │  POST /api/auth/refresh               │                                 │
        │  Cookie: refresh_token=<raw>           │                                 │
        ├───────────────────────────────────────▶│                                 │
        │                                      │  hash(raw) → look up            │
        │                                      │  refresh_tokens.token_hash       │
        │                                      ├──────────────────────────────────▶│
        │                                      │◀──────────────────────────────────┤
        │                                      │                                 │
        │                                      │  found & revoked = true?         │
        │                                      │  → REVOKE ALL tokens for user,   │
        │                                      │    reject with 401 (theft path)  │
        │                                      │                                 │
        │                                      │  found & active (not expired,    │
        │                                      │  not revoked):                   │
        │                                      │   UPDATE ... SET revoked=true,   │
        │                                      │   replaced_by_token_hash=<new>   │
        │                                      ├──────────────────────────────────▶│
        │                                      │   INSERT new refresh_tokens row  │
        │                                      ├──────────────────────────────────▶│
        │                                      │   generate new access token (JWT)│
        │  200 OK + Set-Cookie:                │                                 │
        │  new access_token, new refresh_token │                                 │
        │◀───────────────────────────────────────┤                                 │
```

**Explanation:** this exact exchange happens two ways in this system — (a) explicitly, when the frontend calls `/api/auth/refresh` from `proxy.ts` on behalf of a visitor whose access token has expired but who still has a valid refresh cookie, and (b) it is the same code path a legitimate frontend "silent refresh" or a Postman/manual call would use. The reuse-detection branch (a revoked token being presented again) is a security control, not a normal-path outcome — it only fires if a refresh token is replayed after it has already been rotated away.

## 6. Logout Sequence

```
Browser                    Backend                              PostgreSQL
   │                          │                                       │
   │  POST /api/auth/logout   │                                       │
   │  Cookie: access_token,   │                                       │
   │          refresh_token   │                                       │
   ├─────────────────────────▶│                                       │
   │                          │  JwtAuthenticationFilter validates    │
   │                          │  access_token → sets SecurityContext  │
   │                          │  UPDATE refresh_tokens                │
   │                          │  SET revoked = true                   │
   │                          │  WHERE token_hash = hash(cookie)      │
   │                          ├──────────────────────────────────────▶│
   │                          │  INSERT INTO activity_log             │
   │                          │  (action = LOGOUT)                    │
   │                          ├──────────────────────────────────────▶│
   │  200 OK + Set-Cookie:    │                                       │
   │  access_token=; Max-Age=0│                                       │
   │  refresh_token=; Max-Age=0│                                      │
   │◀─────────────────────────┤                                       │
```

**Explanation:** logout only revokes the **single** refresh token presented in the request — other active sessions (e.g. a different browser) are left alone. Only a password change or reset revokes every session at once. The access token itself is never individually invalidated server-side (it is stateless); logout relies on the cookie being cleared client-side and the token's own short (15-minute) expiry to close the window.

## 7. Request Lifecycle (every API call)

```
Incoming HTTP request
        │
        ▼
┌───────────────────────┐
│ CorsConfig filter      │  rejects/allows based on app.cors.allowed-origins
└───────────┬───────────┘
            ▼
┌───────────────────────┐
│ JwtAuthenticationFilter│  extracts + verifies access_token, loads UserPrincipal,
└───────────┬───────────┘  populates SecurityContext (no-op if no/invalid token)
            ▼
┌───────────────────────┐
│ Spring Security        │  authorizeHttpRequests: public endpoints permitAll,
│ authorization          │  everything else requires authentication;
└───────────┬───────────┘  @PreAuthorize for role checks (e.g. ADMIN on /api/users/**)
            ▼
      ┌─────┴─────┐
      │  allowed?  │
      └─────┬─────┘
       yes  │  no
            │   └────────────────▶ RestAuthenticationEntryPoint (401) or
            │                      RestAccessDeniedHandler (403) — JSON ApiResponse
            ▼
┌───────────────────────┐
│ @RestController method │  Bean Validation on the request DTO runs here
└───────────┬───────────┘
            ▼
┌───────────────────────┐
│ Service layer          │  business logic, audit logging, notifications
└───────────┬───────────┘
            ▼
┌───────────────────────┐
│ Repository / Hibernate │  SQL against PostgreSQL
└───────────┬───────────┘
            ▼
      ┌─────┴─────┐
      │ exception? │
      └─────┬─────┘
       yes  │  no
            │   └────────────────▶ ApiResponse.success(...) → 200/201
            ▼
┌───────────────────────┐
│ GlobalExceptionHandler │  maps ResourceNotFoundException→404, BadRequestException→400,
│ (@RestControllerAdvice)│  ConflictException→409, UnauthorizedException→401,
└───────────────────────┘  validation errors→400, anything else→500
```

## 8. Module Dependency Diagram

```
                         ┌────────────────────┐
                         │   common/           │
                         │  (dto, exception,   │
                         │   audit, notification│
                         │   entity, security,  │
                         │   util, validation)  │
                         └──────────┬───────────┘
                                    │ used by everything below
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
     ┌────────────────┐   ┌──────────────────┐   ┌──────────────────┐
     │ config/          │   │ security/          │   │ modules/settings/│
     │ (CorsConfig,     │   │ (CookieUtil,       │   │ (AppSetting      │
     │  JpaAuditingConfig│──▶│  jwt/JwtToken...,  │   │  scaffold only,  │
     │  OpenApiConfig,  │   │  RateLimiter,      │   │  no dependents   │
     │  SecurityConfig) │   │  Rest*Handler)     │   │  yet)            │
     └────────┬─────────┘   └─────────┬──────────┘   └──────────────────┘
              │                       │
              └───────────┬───────────┘
                          ▼
              ┌────────────────────┐
              │ modules/user/       │
              │ (User, Role,        │
              │  UserPrincipal,     │
              │  UserService,       │
              │  AdminBootstrap)    │
              └──────────┬───────────┘
                          │ AuthServiceImpl authenticates against
                          │ CustomUserDetailsService, issues tokens
                          │ for the resolved User
                          ▼
              ┌────────────────────┐
              │ modules/auth/        │
              │ (AuthController,     │
              │  AuthService,        │
              │  RefreshToken,       │
              │  PasswordResetToken) │
              └────────────────────┘
```

**Explanation:** `common` has no dependency on any feature module — it is the shared foundation. `modules/auth` depends on `modules/user` (it authenticates and issues tokens *for* a `User`), never the other way around. `modules/settings` is a standalone scaffold with no other module depending on it yet. This ordering is why Module 1 could safely reuse `AuditLogService`/`NotificationService` without editing them — a new module can depend "downward" on `common` and, if needed, on `modules/user`, without the reverse ever being true.

## 9. Deployment Architecture

```
┌───────────────────────────────────────────────────────────────────────────┐
│                       Docker Compose network (infra)                      │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐│
│  │  frontend    │    │  backend     │    │  postgres    │    │  minio      ││
│  │  :3000       │───▶│  :8080       │───▶│  :5432       │    │  :9000/9001 ││
│  │  Node 22,    │    │  Java 21,    │    │  postgres:   │    │  minio/minio││
│  │  standalone  │    │  Spring Boot │    │  16-alpine   │    │  :latest    ││
│  │  Next build  │    │  fat jar     │    │  vol:        │    │  vol:       ││
│  │              │    │              │    │  postgres_   │    │  minio_data ││
│  │              │    │              │    │  data        │    │             ││
│  └──────┬───────┘    └──────┬───────┘    └─────────────┘    └─────────────┘│
│         │                   │                                              │
└─────────┼───────────────────┼──────────────────────────────────────────────┘
          │                   │
     host:3000            host:8080   (host:5433 → postgres:5432; MinIO ports pass through unchanged)
          │                   │
          ▼                   ▼
   Developer's browser   curl / Postman / Swagger UI
```

**Explanation:** `depends_on` in `docker-compose.yml` ensures Postgres reports `healthy` (via `pg_isready`) before the backend container starts, and MinIO has at least started (no health check defined for it yet, since nothing consumes it). The frontend depends on the backend starting. Postgres's host-exposed port is deliberately remapped to `5433` by default to avoid colliding with any other local Postgres instance; internally, on the Compose network, it is still reached at `postgres:5432`. The frontend container reaches the backend at `http://backend:8080/api` for its own server-side calls, while the developer's actual browser reaches it at `http://localhost:8080/api` — these are two different URLs for two different callers, both pointing at the same backend container.
