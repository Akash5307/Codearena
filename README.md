# CodeArena

Competitive programming platform backend (Codeforces clone) built with Java 21 + Spring Boot 3.3.

## Prerequisites

- Java 21
- Docker & Docker Compose

## Quick Start

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
- Runs code in isolated Docker containers (gcc:13, openjdk:21, python:3.12, etc.)
- Enforces CPU, memory, and time limits per problem
- Downloads test cases from MinIO, compares output (exact match, trimmed)
- Publishes verdict back via `judge.result.queue`

Supported languages: Java, C++, C, Python, JavaScript, Go, Rust, Kotlin.

### 4. Stop infrastructure

```bash
docker compose down
```

Add `-v` to also remove data volumes.

### 5. Production deployment (Docker)

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

This builds and runs both the API and Judge Worker alongside infrastructure services. See the deployment guide (`DEPLOYMENT.md`) for full details.

## Project Structure

```
codearena/
├── docker-compose.yml          # Infrastructure only (local dev)
├── docker-compose.prod.yml     # Full stack (infra + app services)
├── codearena-api/              # Main Spring Boot API
│   └── Dockerfile              # Multi-stage production build
└── codearena-judge/            # Judge worker (submission evaluator)
    └── Dockerfile              # Multi-stage production build
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
./gradlew build            # compile + test
./gradlew :codearena-api:test   # run API tests only (61 tests)
```

## Key Features

- **Structured error responses** with error codes (`VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, etc.)
- **Redis caching** on hot paths (problem detail, tags, user profiles) with auto-eviction on writes
- **Structured JSON logging** (production profile) with correlation IDs via `X-Correlation-ID` header
- **Full Swagger documentation** with `@Schema` examples on all request/response DTOs
- **Multi-stage Docker builds** for minimal production images (JRE-only runtime)
