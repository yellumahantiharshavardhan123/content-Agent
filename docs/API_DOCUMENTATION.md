# API Documentation — Arjun Sports AI Content Agent

**Base URL (local):** `http://localhost:8080/api`
**Scope:** only endpoints that exist in the codebase today (Module 0 health check + Module 1 auth/user endpoints). Interactive documentation is also available at `/swagger-ui.html` and `/api-docs` when the `dev` Spring profile is active (both are disabled in the `prod` profile).

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
| 401 | Unauthorized | Missing/invalid/expired token, wrong credentials, `UnauthorizedException`, Spring Security's `RestAuthenticationEntryPoint` |
| 403 | Forbidden | Authenticated but wrong role — `RestAccessDeniedHandler` |
| 404 | Not found | `ResourceNotFoundException` |
| 409 | Conflict | `ConflictException` (e.g. duplicate email) |
| 429 | Too many requests | Login rate limiter exceeded |
| 500 | Internal server error | Any uncaught exception (`GlobalExceptionHandler` fallback) |

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
