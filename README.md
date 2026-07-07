# Arjun Sports AI Content Agent

AI-powered content marketing system for Arjun Sports Shooting Academy: upload media, generate AI content (Instagram captions, hashtags, blog articles, SEO metadata, reel scripts), review and approve it, then publish to Instagram and the academy website — with a full audit trail and in-app notifications throughout.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind CSS, shadcn/ui |
| Backend | Spring Boot 3, Java 21, Spring Security, Spring Data JPA |
| Database | PostgreSQL (Flyway-managed migrations) |
| Storage | MinIO (S3-compatible) |
| Auth | JWT |
| AI | OpenAI-compatible API (configurable base URL/model) |
| Deployment | Docker / Docker Compose |

## Monorepo layout

```
├── frontend/   Next.js app (admin dashboard)
├── backend/    Spring Boot API
├── infra/      docker-compose.yml and local infra
├── docs/       API docs, Postman collection (added as modules ship)
└── .env.example
```

## Build status

This project ships module-by-module; see the build plan for the full roadmap. Shipped so far:

- **Module 0 — Foundation & Scaffolding**: monorepo skeleton, Spring Boot bootstrap (security baseline, global exception handling, `ApiResponse`/`PageResponse` wrappers, Flyway), cross-cutting audit log and in-app notification services, `AppSetting` entity scaffold, Next.js bootstrap with dark/light theme and a dashboard shell navigable to every future module.

Everything else (auth, media upload, AI generation, drafts, approvals, Instagram/website publishing, scheduler, analytics, activity log viewer, notification center, settings UI) ships in later modules.

## Local development

### Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+
- Docker (for Postgres/MinIO, or the full stack)

### Quick start — full stack via Docker

```bash
cp .env.example .env
# edit .env: set POSTGRES_PASSWORD, STORAGE_SECRET_KEY, JWT_SECRET, AI_PROVIDER_API_KEY, etc.

docker compose --env-file .env -f infra/docker-compose.yml up -d --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api (health check: `GET /api/health`)
- Swagger UI (dev profile): http://localhost:8080/swagger-ui.html
- MinIO console: http://localhost:9001

### Backend only (native, against Docker Postgres/MinIO)

```bash
docker compose --env-file .env -f infra/docker-compose.yml up -d postgres minio
cd backend
mvn spring-boot:run
```

### Frontend only

```bash
cd frontend
npm install
npm run dev
```

## Environment variables

See [`.env.example`](.env.example) for the full list (database, JWT, storage, AI provider, Instagram Graph API, CORS). Never commit a real `.env` file.

## Testing

```bash
# Backend unit tests
cd backend && mvn test

# Frontend lint + build
cd frontend && npm run lint && npm run build
```
