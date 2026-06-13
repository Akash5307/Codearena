# CodeArena

Full-stack competitive programming platform (Codeforces clone): Java 21 + Spring Boot 3.3 backend, React + TypeScript + Tailwind frontend, Docker-sandboxed judge.

## Prerequisites

- Docker & Docker Compose (that's all for the full-stack run)
- Java 21 + Node 22 only if running services outside Docker

## Quick Start (everything in Docker)

```bash
docker compose -f docker-compose.full.yml up --build
```

Starts the whole stack: PostgreSQL, Redis, RabbitMQ, MinIO, the API, the judge
worker, and the frontend. First build takes a few minutes.

| Service     | URL                                            |
|-------------|------------------------------------------------|
| Frontend    | http://localhost:3000                          |
| API         | http://localhost:8080 (Swagger: `/swagger-ui.html`) |
| Judge       | http://localhost:8081                          |
| RabbitMQ UI | http://localhost:15672 (guest/guest)           |
| MinIO console | http://localhost:9001 (minioadmin/minioadmin)|

The frontend's nginx reverse-proxies `/api` to the API container, so the browser
stays same-origin (the backend has no CORS config — by design).

> Judge note: the worker talks to the **host** Docker daemon via
> `/var/run/docker.sock` and stages submissions in `/tmp/codearena-judge`, which
> is bind-mounted at the same absolute path on host and container (sandbox bind
> mounts are resolved by the host daemon — see `design.md`).

## Dev Mode (services on the host)

### 1. Start infrastructure services

```bash
docker compose up -d
```

This starts PostgreSQL 16, Redis, RabbitMQ, and MinIO.

| Service    | URL / Port                          |
|------------|-------------------------------------|
| PostgreSQL | `localhost:5432`                    |
| Redis      | `localhost:6379`                    |
| RabbitMQ   | `localhost:5672` (mgmt: `15672`)    |
| MinIO      | `localhost:9000` (console: `9001`)  |

### 2. Run the API server

```bash
./gradlew :codearena-api:bootRun
```

The API starts on **http://localhost:8080**.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### 3. Run the Judge Worker

```bash
./gradlew :codearena-judge:bootRun
```

Runs on port `8081`. Requires Docker daemon running on the host.

The judge worker:
- Consumes submission jobs from RabbitMQ (`judge.queue`)
- Runs code in isolated Docker containers (gcc:13, eclipse-temurin:21, python:3.12, etc.), hardened: network disabled, all capabilities dropped, `no-new-privileges`, PID-limited (fork-bomb guard), output-capped (flood guard)
- Enforces CPU, memory, and wall-clock time limits per problem; reports real per-run time
- Pre-pulls all sandbox images on startup so the first submission per language doesn't stall
- Downloads test cases from MinIO, compares output (exact match, trimmed)
- Emits a `JUDGING` progress signal on pickup, then publishes the verdict via `judge.result.queue`

Verdicts: `AC`, `WA`, `TLE`, `MLE` (cgroup OOM detection), `RE`, `CE`.

Supported languages: Java, C++, C, Python, JavaScript, Go, Rust, Kotlin.
(Kotlin runs in a custom `codearena-kotlin:21` sandbox image — build it once on the judge host: `docker build -t codearena-kotlin:21 codearena-judge/sandbox-images/kotlin`. See DEPLOYMENT.md.)

### 4. Run the frontend (dev)

```bash
cd frontend && npm install && npm run dev
```

Vite dev server on **http://localhost:5173**, proxies `/api` to `:8080`.

### 5. Stop infrastructure

```bash
docker compose down
```

Add `-v` to also remove data volumes.

## Project Structure

```
codearena/
├── docker-compose.yml          # Infrastructure only (local dev)
├── docker-compose.full.yml     # Whole stack in Docker (infra + api + judge + frontend)
├── codearena-api/              # Main Spring Boot API
│   └── Dockerfile              # Multi-stage build (gradle image → JRE-only runtime)
├── codearena-judge/            # Judge worker (submission evaluator)
│   └── Dockerfile              # Multi-stage build; runs as root for docker.sock access
└── frontend/                   # React + TS + Tailwind SPA
    └── Dockerfile              # Node build → nginx serve (+ /api reverse proxy)
```

## API Endpoints

### Auth (`/api/v1/auth`)

| Method | Path        | Description             | Auth |
|--------|-------------|-------------------------|------|
| POST   | `/register` | Register new user       | No   |
| POST   | `/login`    | Login, returns JWT pair | No   |
| POST   | `/refresh`  | Refresh access token    | No   |
| POST   | `/logout`   | Invalidate tokens       | Yes  |

### Users (`/api/v1/users`)

| Method | Path                      | Description              | Auth |
|--------|---------------------------|--------------------------|------|
| GET    | `/{username}`             | Get user profile         | No   |
| GET    | `/{username}/submissions` | User's submission history| No   |
| PUT    | `/me`                     | Update own profile       | Yes  |
| GET    | `/ratings`                | Rating leaderboard       | No   |

### Problems (`/api/v1/problems`)

| Method | Path              | Description                          | Auth           |
|--------|-------------------|--------------------------------------|----------------|
| GET    | `/`               | List problems (filter, search, page) | No             |
| GET    | `/{slug}`         | Get problem detail                   | No             |
| GET    | `/{slug}/samples` | Sample test cases (actual I/O text)  | No             |
| POST   | `/`               | Create problem                       | PROBLEM_SETTER |
| PUT    | `/{id}`           | Update problem                       | PROBLEM_SETTER |
| POST   | `/{id}/test-cases`| Upload test case (multipart)         | PROBLEM_SETTER |
| GET    | `/tags`           | List all tags                        | No             |

### Contests (`/api/v1/contests`)

| Method | Path               | Description                           | Auth  |
|--------|--------------------|---------------------------------------|-------|
| GET    | `/`                | List contests (upcoming/running/past) | No    |
| GET    | `/{slug}`          | Contest detail + problems             | No    |
| POST   | `/`                | Create contest                        | ADMIN |
| POST   | `/{id}/register`   | Register for contest                  | Yes   |
| GET    | `/{id}/standings`  | Live/final standings (ICPC-style)     | No    |
| GET    | `/{id}/my-submissions` | My submissions in contest         | Yes   |

### Submissions (`/api/v1/submissions`)

| Method | Path   | Description                            | Auth |
|--------|--------|----------------------------------------|------|
| POST   | `/`    | Submit solution (rate limited: 1/10s)  | Yes  |
| GET    | `/{id}`| Submission detail + verdict            | Yes  |
| GET    | `/`    | Recent submissions (filterable, paged) | No   |

Filters: `userId`, `problemId`, `language`, `verdict` query params.

### Blogs (`/api/v1/blogs`)

| Method | Path              | Description                              | Auth |
|--------|-------------------|------------------------------------------|------|
| GET    | `/`               | List blog posts (paginated)              | No   |
| GET    | `/{id}`           | Blog post detail with threaded comments  | No   |
| POST   | `/`               | Create blog post                         | Yes  |
| PUT    | `/{id}`           | Edit own blog post                       | Yes  |
| POST   | `/{id}/vote`      | Upvote/downvote (toggleable)             | Yes  |
| POST   | `/{id}/comments`  | Add comment (supports threaded replies)  | Yes  |

Authentication uses JWT Bearer tokens. Include `Authorization: Bearer <token>` header for protected endpoints.

## Build & Test

```bash
./gradlew build                 # backend: compile + test (75 tests: 61 API + 14 judge)
cd frontend && npm run build    # frontend: typecheck (tsc) + vite production build
```

CI runs all of this on push/PR via `.github/workflows/ci.yml` (backend tests, frontend build, Docker image builds).

## Key Features

- **React + TypeScript SPA** (Vite + Tailwind, Codeforces-style): problem browsing with Markdown + KaTeX statements, code submission with **live verdict polling**, submission history, contests with live standings, rating-colored profiles, role-gated **admin tooling** (create problems/contests, multipart test-case upload, publish), and a dependency-free auto-indenting code editor
- **Hardened sandboxed judging**: every submission compiles and runs in a Docker container with no network, all capabilities dropped, `no-new-privileges`, a PID cap (fork-bomb safe) and output cap (flood safe), plus CPU/memory/time limits; resilient to worker crashes (durable RabbitMQ queue + redelivery, verified)
- **All six verdicts**: AC / WA / TLE / MLE (cgroup OOM detection) / RE / CE, with `JUDGING` progress signalled on pickup
- **ICPC standings** with Redis caching, invalidated on accepted contest submissions
- **Codeforces-style rating engine** — when a rated contest ends, ratings are recomputed (seed → performance → delta, with anti-inflation passes), applied idempotently, and recorded per-contest; runs automatically (scheduler) or via an admin trigger
- **Structured error responses** with error codes (`VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, etc.)
- **Redis caching** on hot paths (problem detail, tags, user profiles) with auto-eviction on writes
- **Structured JSON logging** (production profile) with correlation IDs via `X-Correlation-ID` header
- **Full Swagger documentation** with `@Schema` examples on all request/response DTOs
- **Multi-stage Docker builds** for minimal production images (JRE-only runtime)

## Known Gaps

- Verdict/standings updates are poll-based, not pushed (no WebSocket) — a deliberate choice to keep the API stateless
- No plagiarism detection on submissions
