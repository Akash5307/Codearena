# CLAUDE.md — CodeArena (Codeforces Clone Backend)

## Project Overview

**CodeArena** is a competitive programming platform (Codeforces clone) backend built with Java 21 + Spring Boot 3.x. It supports user registration, problem management, contest hosting, code submission with automated judging, real-time leaderboards, and editorial/blog features.
Use Java not Kotlin.
---

## Tech Stack

| Layer              | Technology                                      |
|--------------------|--------------------------------------------------|
| Language           | Java 21                                          |
| Framework          | Spring Boot 3.3+                                 |
| Build Tool         | Gradle                               |
| Database           | PostgreSQL 16                                    |
| Caching            | Redis                                            |
| ORM                | Spring Data JPA / Hibernate                      |
| Auth               | Spring Security + JWT (access + refresh tokens)  |
| API Docs           | SpringDoc OpenAPI (Swagger UI)                   |
| Migration          | Flyway                                           |
| Messaging/Queue    | RabbitMQ (for judge job queue)                   |
| Object Storage     | MinIO (S3-compatible, for test cases)            |
| Containerization   | Docker + Docker Compose                          |
| Testing            | JUnit 5 + Testcontainers + MockMvc               |

---

## Architecture Principles

- **Modular monolith**: Organize code by domain module (user, problem, contest, submission, judge), not by technical layer. Each module has its own controller/service/repository/dto/entity packages.
- **Judge is a separate worker process**: The main API publishes submission jobs to RabbitMQ. A separate `judge-worker` Spring Boot app (or module) consumes jobs, runs code in a sandboxed Docker container, and publishes results back.
- **DTOs everywhere at the boundary**: Never expose JPA entities in API responses. Use records as DTOs.
- **Flyway for all schema changes**: No `ddl-auto=update`. Every schema change is a numbered migration.
- **Pagination by default**: All list endpoints return `Page<T>`.

---

## Project Structure

```
codearena/
├── docker-compose.yml
├── codearena-api/                        # Main Spring Boot API
│   ├── build.gradle.kts
│   └── src/main/java/com/codearena/
│       ├── CodeArenaApplication.java
│       ├── config/                        # Security, Redis, RabbitMQ, OpenAPI config
│       │   ├── SecurityConfig.java
│       │   ├── JwtConfig.java
│       │   ├── RedisConfig.java
│       │   ├── RabbitMQConfig.java
│       │   └── OpenApiConfig.java
│       ├── common/                        # Shared utilities
│       │   ├── exception/                 # Global exception handler, custom exceptions
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── ResourceNotFoundException.java
│       │   │   └── BusinessException.java
│       │   ├── dto/
│       │   │   └── ApiResponse.java       # Uniform { success, data, error } wrapper
│       │   └── util/
│       ├── user/                          # --- USER MODULE ---
│       │   ├── controller/
│       │   │   └── UserController.java
│       │   │   └── AuthController.java
│       │   ├── service/
│       │   │   └── UserService.java
│       │   │   └── AuthService.java
│       │   ├── repository/
│       │   │   └── UserRepository.java
│       │   ├── entity/
│       │   │   └── User.java
│       │   └── dto/
│       │       ├── RegisterRequest.java
│       │       ├── LoginRequest.java
│       │       ├── UserProfileResponse.java
│       │       └── TokenResponse.java
│       ├── problem/                       # --- PROBLEM MODULE ---
│       │   ├── controller/
│       │   │   └── ProblemController.java
│       │   ├── service/
│       │   │   └── ProblemService.java
│       │   ├── repository/
│       │   │   ├── ProblemRepository.java
│       │   │   └── TestCaseRepository.java
│       │   ├── entity/
│       │   │   ├── Problem.java
│       │   │   ├── TestCase.java
│       │   │   └── Tag.java
│       │   └── dto/
│       │       ├── ProblemCreateRequest.java
│       │       ├── ProblemListResponse.java
│       │       └── ProblemDetailResponse.java
│       ├── contest/                       # --- CONTEST MODULE ---
│       │   ├── controller/
│       │   │   └── ContestController.java
│       │   ├── service/
│       │   │   ├── ContestService.java
│       │   │   └── StandingsService.java
│       │   ├── repository/
│       │   │   ├── ContestRepository.java
│       │   │   └── ContestRegistrationRepository.java
│       │   ├── entity/
│       │   │   ├── Contest.java
│       │   │   ├── ContestProblem.java
│       │   │   └── ContestRegistration.java
│       │   └── dto/
│       │       ├── ContestCreateRequest.java
│       │       ├── ContestListResponse.java
│       │       ├── ContestDetailResponse.java
│       │       └── StandingsResponse.java
│       ├── submission/                    # --- SUBMISSION MODULE ---
│       │   ├── controller/
│       │   │   └── SubmissionController.java
│       │   ├── service/
│       │   │   ├── SubmissionService.java
│       │   │   └── SubmissionPublisher.java
│       │   ├── repository/
│       │   │   └── SubmissionRepository.java
│       │   ├── entity/
│       │   │   └── Submission.java
│       │   └── dto/
│       │       ├── SubmitRequest.java
│       │       ├── SubmissionListResponse.java
│       │       └── SubmissionDetailResponse.java
│       └── blog/                          # --- BLOG/EDITORIAL MODULE ---
│           ├── controller/
│           │   └── BlogController.java
│           ├── service/
│           │   └── BlogService.java
│           ├── repository/
│           │   └── BlogPostRepository.java
│           ├── entity/
│           │   ├── BlogPost.java
│           │   └── Comment.java
│           └── dto/
├── codearena-judge/                       # Separate judge worker
│   ├── build.gradle.kts
│   └── src/main/java/com/codearena/judge/
│       ├── JudgeWorkerApplication.java
│       ├── consumer/
│       │   └── SubmissionConsumer.java
│       ├── sandbox/
│       │   └── DockerSandbox.java
│       ├── service/
│       │   └── JudgeService.java
│       └── dto/
│           ├── JudgeTask.java
│           └── JudgeResult.java
└── sql/
    └── migrations/                        # Flyway migrations
        ├── V1__create_users.sql
        ├── V2__create_problems.sql
        ├── V3__create_contests.sql
        ├── V4__create_submissions.sql
        └── V5__create_blogs.sql
```

---

## Data Model (Core Entities)

### Users
```
users
├── id              BIGSERIAL PK
├── username        VARCHAR(30) UNIQUE NOT NULL
├── email           VARCHAR(255) UNIQUE NOT NULL
├── password_hash   VARCHAR(255) NOT NULL
├── role            VARCHAR(20) DEFAULT 'USER'   -- USER, ADMIN, PROBLEM_SETTER
├── rating          INT DEFAULT 1500
├── max_rating      INT DEFAULT 1500
├── avatar_url      VARCHAR(500)
├── created_at      TIMESTAMP
└── updated_at      TIMESTAMP
```

### Problems
```
problems
├── id              BIGSERIAL PK
├── title           VARCHAR(255) NOT NULL
├── slug            VARCHAR(255) UNIQUE NOT NULL
├── statement       TEXT NOT NULL                 -- Markdown
├── input_format    TEXT
├── output_format   TEXT
├── difficulty       VARCHAR(20)                  -- EASY, MEDIUM, HARD
├── time_limit_ms   INT DEFAULT 2000
├── memory_limit_mb INT DEFAULT 256
├── author_id       BIGINT FK -> users(id)
├── is_published    BOOLEAN DEFAULT false
├── created_at      TIMESTAMP
└── updated_at      TIMESTAMP

test_cases
├── id              BIGSERIAL PK
├── problem_id      BIGINT FK -> problems(id)
├── input_url       VARCHAR(500)                 -- MinIO path
├── expected_output_url VARCHAR(500)             -- MinIO path
├── is_sample       BOOLEAN DEFAULT false
├── order_index     INT
└── created_at      TIMESTAMP

tags
├── id              BIGSERIAL PK
└── name            VARCHAR(50) UNIQUE NOT NULL

problem_tags (join table)
├── problem_id      BIGINT FK
└── tag_id          BIGINT FK
```

### Contests
```
contests
├── id              BIGSERIAL PK
├── title           VARCHAR(255) NOT NULL
├── slug            VARCHAR(255) UNIQUE NOT NULL
├── description     TEXT
├── type            VARCHAR(20)                  -- ICPC, IOI, EDUCATIONAL
├── start_time      TIMESTAMP NOT NULL
├── duration_minutes INT NOT NULL
├── is_rated        BOOLEAN DEFAULT true
├── author_id       BIGINT FK -> users(id)
├── created_at      TIMESTAMP
└── updated_at      TIMESTAMP

contest_problems
├── id              BIGSERIAL PK
├── contest_id      BIGINT FK -> contests(id)
├── problem_id      BIGINT FK -> problems(id)
├── label           VARCHAR(5)                   -- A, B, C, ...
├── order_index     INT
└── points          INT                          -- for IOI-style

contest_registrations
├── id              BIGSERIAL PK
├── contest_id      BIGINT FK -> contests(id)
├── user_id         BIGINT FK -> users(id)
├── registered_at   TIMESTAMP
└── UNIQUE(contest_id, user_id)
```

### Submissions
```
submissions
├── id              BIGSERIAL PK
├── user_id         BIGINT FK -> users(id)
├── problem_id      BIGINT FK -> problems(id)
├── contest_id      BIGINT FK -> contests(id) NULLABLE
├── language        VARCHAR(20) NOT NULL         -- JAVA, CPP, PYTHON, etc.
├── source_code     TEXT NOT NULL
├── verdict         VARCHAR(30) DEFAULT 'PENDING' -- PENDING, JUDGING, AC, WA, TLE, MLE, RE, CE
├── time_used_ms    INT
├── memory_used_kb  INT
├── test_cases_passed INT DEFAULT 0
├── total_test_cases  INT DEFAULT 0
├── submitted_at    TIMESTAMP
└── judged_at       TIMESTAMP
```

### Blogs
```
blog_posts
├── id              BIGSERIAL PK
├── author_id       BIGINT FK -> users(id)
├── title           VARCHAR(255) NOT NULL
├── content         TEXT NOT NULL                 -- Markdown
├── upvotes         INT DEFAULT 0
├── downvotes       INT DEFAULT 0
├── created_at      TIMESTAMP
└── updated_at      TIMESTAMP

comments
├── id              BIGSERIAL PK
├── blog_post_id    BIGINT FK -> blog_posts(id)
├── author_id       BIGINT FK -> users(id)
├── parent_id       BIGINT FK -> comments(id) NULLABLE  -- threaded replies
├── content         TEXT NOT NULL
├── created_at      TIMESTAMP
└── updated_at      TIMESTAMP
```

---

## API Endpoints

### Auth (`/api/v1/auth`)
| Method | Path         | Description              | Auth  |
|--------|-------------|--------------------------|-------|
| POST   | /register   | Register new user         | No    |
| POST   | /login      | Login, returns JWT pair   | No    |
| POST   | /refresh    | Refresh access token      | No    |
| POST   | /logout     | Invalidate refresh token  | Yes   |

### Users (`/api/v1/users`)
| Method | Path                  | Description                  | Auth  |
|--------|-----------------------|------------------------------|-------|
| GET    | /{username}           | Get user profile              | No    |
| GET    | /{username}/submissions | User's submission history   | No    |
| PUT    | /me                   | Update own profile            | Yes   |
| GET    | /ratings              | Rating leaderboard (paged)   | No    |

### Problems (`/api/v1/problems`)
| Method | Path              | Description                       | Auth           |
|--------|-------------------|-----------------------------------|----------------|
| GET    | /                 | List problems (filter, search, page) | No          |
| GET    | /{slug}           | Get problem detail                | No             |
| POST   | /                 | Create problem                    | PROBLEM_SETTER |
| PUT    | /{id}             | Update problem                    | PROBLEM_SETTER |
| POST   | /{id}/test-cases  | Upload test cases                 | PROBLEM_SETTER |
| GET    | /tags             | List all tags                     | No             |

### Contests (`/api/v1/contests`)
| Method | Path                      | Description                  | Auth    |
|--------|---------------------------|------------------------------|---------|
| GET    | /                         | List contests (upcoming/past)| No      |
| GET    | /{slug}                   | Contest detail + problems    | No      |
| POST   | /                         | Create contest               | ADMIN   |
| POST   | /{id}/register            | Register for contest         | Yes     |
| GET    | /{id}/standings           | Live/final standings         | No      |
| GET    | /{id}/my-submissions      | My submissions in contest    | Yes     |

### Submissions (`/api/v1/submissions`)
| Method | Path              | Description                           | Auth  |
|--------|-------------------|---------------------------------------|-------|
| POST   | /                 | Submit solution                       | Yes   |
| GET    | /{id}             | Submission detail + verdict           | Yes   |
| GET    | /                 | Recent submissions (paged, filterable)| No    |

### Blogs (`/api/v1/blogs`)
| Method | Path                      | Description              | Auth  |
|--------|---------------------------|--------------------------|-------|
| GET    | /                         | List posts (paged)        | No    |
| GET    | /{id}                     | Post detail + comments    | No    |
| POST   | /                         | Create post               | Yes   |
| PUT    | /{id}                     | Edit post                 | Yes   |
| POST   | /{id}/vote                | Upvote/downvote           | Yes   |
| POST   | /{id}/comments            | Add comment               | Yes   |

---

## Build Phases (Step-by-Step)

### Phase 1: Project Skeleton & Infrastructure
**Goal**: Running Spring Boot app with DB, Redis, RabbitMQ, all wired up.

- [ ] Initialize Gradle multi-module project (`codearena-api`, `codearena-judge`)
- [ ] Set up `docker-compose.yml` with PostgreSQL 16, Redis, RabbitMQ, MinIO
- [ ] Configure `application.yml` for all service connections
- [ ] Set up Flyway with initial migration structure
- [ ] Create `ApiResponse<T>` wrapper record and `GlobalExceptionHandler`
- [ ] Configure SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- [ ] Verify everything starts with `docker compose up` + `./gradlew bootRun`

### Phase 2: User Module & Authentication
**Goal**: Users can register, login, and access protected endpoints.

- [ ] Flyway migration `V1__create_users.sql`
- [ ] `User` entity with JPA annotations
- [ ] `UserRepository` extending `JpaRepository`
- [ ] `AuthService` — register (bcrypt hash), login (return JWT pair), refresh
- [ ] JWT utility class (generate, validate, extract claims)
- [ ] `JwtAuthenticationFilter` (OncePerRequestFilter)
- [ ] `SecurityConfig` — permit auth endpoints, protect others, stateless session
- [ ] `AuthController` with register/login/refresh/logout endpoints
- [ ] `UserController` with profile and update endpoints
- [ ] `UserService` for profile operations and rating leaderboard
- [ ] Unit tests for AuthService, integration tests for AuthController

### Phase 3: Problem Module
**Goal**: Problem setters can create problems with test cases; users can browse them.

- [ ] Flyway migrations `V2__create_problems.sql` (problems, test_cases, tags, problem_tags)
- [ ] `Problem`, `TestCase`, `Tag` entities
- [ ] `ProblemRepository` with custom queries (filter by tag, difficulty, search by title)
- [ ] `ProblemService` with pagination, slug generation, and tag management
- [ ] `ProblemController` with all CRUD + test case upload endpoints
- [ ] MinIO integration for test case file upload/download
- [ ] Role-based access: only `PROBLEM_SETTER` and `ADMIN` can create/edit
- [ ] Integration tests

### Phase 4: Contest Module
**Goal**: Admins can create contests with problems; users register and see standings.

- [ ] Flyway migration `V3__create_contests.sql`
- [ ] `Contest`, `ContestProblem`, `ContestRegistration` entities
- [ ] `ContestRepository` with queries for upcoming, running, past contests
- [ ] `ContestService` — create, register, validate contest timing
- [ ] `StandingsService` — compute standings (ICPC-style: solved count + penalty time)
- [ ] Redis caching for standings (invalidate on new accepted submission)
- [ ] `ContestController` with all endpoints
- [ ] Contest state machine: BEFORE → RUNNING → ENDED (based on time)
- [ ] Integration tests

### Phase 5: Submission Module & Judge Queue
**Goal**: Users submit code, submissions are queued and picked up by judge.

- [ ] Flyway migration `V4__create_submissions.sql`
- [ ] `Submission` entity
- [ ] `SubmissionService` — validate, save, publish to RabbitMQ
- [ ] `SubmissionPublisher` — serialize `JudgeTask` and send to `judge.queue`
- [ ] `SubmissionController` with submit + list + detail endpoints
- [ ] Rate limiting on submission endpoint (e.g., 1 submission per 10 seconds per user)
- [ ] Integration tests (mock the queue)

### Phase 6: Judge Worker
**Goal**: Separate process consumes judge tasks, runs code in Docker sandbox, returns results.

- [ ] `codearena-judge` Spring Boot module with RabbitMQ consumer
- [ ] `SubmissionConsumer` — listens on `judge.queue`
- [ ] `DockerSandbox` — uses Docker Java API to:
    - Pull language-specific images (openjdk:21, gcc:13, python:3.12)
    - Mount source code as a volume
    - Set CPU/memory limits and timeout
    - Run compile + execute steps
    - Capture stdout/stderr
- [ ] `JudgeService` — orchestrates:
    1. Download test cases from MinIO
    2. Compile source (if needed)
    3. Run against each test case with time/memory limits
    4. Compare output (exact match, trimmed)
    5. Determine verdict (AC, WA, TLE, MLE, RE, CE)
    6. Publish `JudgeResult` back to API via RabbitMQ callback queue or REST
- [ ] Update `Submission` record with verdict, time, memory
- [ ] Invalidate standings cache on AC verdict during active contest

### Phase 7: Blog & Editorial Module
**Goal**: Users can write blog posts and editorials, comment, and vote.

- [ ] Flyway migration `V5__create_blogs.sql`
- [ ] `BlogPost`, `Comment` entities
- [ ] `BlogService` with CRUD, voting (prevent double-vote), threaded comments
- [ ] `BlogController` with all endpoints
- [ ] Integration tests

### Phase 8: Hardening & Polish
**Goal**: Production-ready quality.

- [ ] Rate limiting (Spring Cloud Gateway or Bucket4j)
- [ ] Input validation (Jakarta Validation annotations on all DTOs)
- [ ] Comprehensive error codes in `ApiResponse`
- [ ] Pagination + sorting on all list endpoints
- [ ] Database indexes on hot query paths (submissions by user+problem, contest standings)
- [ ] Redis caching strategy: user profiles, problem lists, standings
- [ ] Structured logging (JSON logs with correlation IDs)
- [ ] Health check endpoints (`/actuator/health`)
- [ ] API versioning (`/api/v1/...`)
- [ ] Docker multi-stage build for production image
- [ ] Full Swagger documentation with examples on all endpoints
- [ ] Load testing with k6 or Gatling on submission + standings endpoints

---

## Coding Conventions

- **DTOs**: Use Java `record` types. Name pattern: `XxxRequest`, `XxxResponse`.
- **Entities**: Use Lombok `@Getter @Setter @NoArgsConstructor`. No `@Data` on entities (breaks equals/hashCode for JPA).
- **Services**: Constructor injection (no `@Autowired` on fields).
- **Controllers**: Thin — only validation + delegation to service + response wrapping.
- **Exceptions**: Throw custom exceptions (`ResourceNotFoundException`, `BusinessException`), let `GlobalExceptionHandler` translate to HTTP status + `ApiResponse`.
- **Naming**: REST endpoints use kebab-case paths, JSON uses camelCase.
- **Tests**: Every service gets unit tests. Every controller gets `@WebMvcTest` integration test. Judge gets `@Testcontainers` test.

---

## Environment Variables

```yaml
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=codearena
POSTGRES_USER=codearena
POSTGRES_PASSWORD=changeme

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# MinIO
MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=codearena-testcases

# JWT
JWT_SECRET=your-256-bit-secret-key-here
JWT_ACCESS_EXPIRY_MS=900000        # 15 minutes
JWT_REFRESH_EXPIRY_MS=604800000    # 7 days
```

---

# FRONTEND (in `frontend/`)

Backend AND frontend core loop are **done and verified end-to-end in Docker**
(~70 checks: all endpoints, all 6 verdicts, 4 languages, concurrency, worker
crash redelivery, restart persistence). Stack: **Vite + React + TypeScript +
Tailwind v3**, router = react-router-dom, UI direction = **Codeforces-like**
(dense, light theme, CF red/blue accents, rating-colored usernames, verdict
colors). Implemented scope = **core judge loop** (auth → problems with real
sample I/O → submit → verdict → submissions). Contests/blogs/leaderboard pages
still deferred (the backend for them works and is tested).

## Backend integration facts (critical)

- API base `http://localhost:8080`, routes under `/api/v1`.
- **No CORS on the backend** and `.cors()` is NOT enabled in the security chain.
  → Frontend uses a **Vite dev proxy**: `/api` → `http://localhost:8080`. The
  browser only ever hits the Vite origin (:5173). For prod, add CORS or serve
  the SPA same-origin behind a reverse proxy.
- Every response is wrapped: `{ success, data, errorCode, error }`.
- Paginated `data` is a Spring `Page`: `{ content[], totalElements, totalPages,
  number, size }`. Query params: `page` (0-based), `size`, `sort=field,dir`.
- Auth = JWT pair. login/register → `{accessToken, refreshToken, tokenType,
  expiresIn}`. No `/auth/me`; decode the JWT payload client-side for
  `userId/username/role`. `/auth/refresh` blacklists the old refresh token.
  `/auth/logout` needs the Bearer header + `{refreshToken}` body.
- Verdicts: `PENDING, JUDGING, AC, WA, TLE, MLE, RE, CE` (PENDING/JUDGING ⇒ keep
  polling `GET /submissions/{id}`). MLE comes from the container's OOMKilled
  flag. Difficulty enum is UPPERCASE. Languages:
  `JAVA, CPP, C, PYTHON, JAVASCRIPT, GO, RUST, KOTLIN` (Kotlin runs via the
  custom codearena-kotlin:21 image — build it on the judge host).
- Sample I/O: `GET /problems/{slug}/samples` (public) returns
  `[{id, orderIndex, input, output}]` — actual text, samples only, hidden test
  cases never served. ProblemDetail renders these in a CF-style examples grid.
  (`sampleTestCases` on the problem detail itself are still MinIO object keys.)

## Frontend structure (`frontend/src/`)

```
lib/api.ts        fetch client: wraps /api, attaches Bearer, auto-refresh on 401,
                  unwraps ApiResponse, throws ApiError{code,message}
lib/types.ts      TS mirrors of backend DTOs + Page<T> + Verdict/Language/Difficulty
lib/jwt.ts        decodeJwt() for current-user identity
store/auth.tsx    AuthProvider/useAuth: login/register/logout, persists tokens in
                  localStorage, exposes current user
components/        Layout, Navbar, ProtectedRoute, VerdictBadge, DifficultyBadge,
                  RatingName, Pagination, CodeEditor (smart textarea: auto-indent,
                  bracket/quote auto-close, indent-aware Tab/Backspace)
pages/            Login, Register, ProblemList, ProblemDetail (statement +
                  submit panel), Submissions (status), SubmissionDetail, Profile
App.tsx           routes;  main.tsx mounts AuthProvider + Router
```

Decisions: code editor = smart `<textarea>` (auto-indentation that carries leading
whitespace + adds a level after `{`/`[`/`(`/`:`, bracket & quote auto-closing with
step-over, indent-aware Tab/Shift-Tab and Backspace, scroll-synced line gutter).
Dependency-free on purpose — no Monaco yet; lean + offline-safe; upgrade later. Statements rendered via react-markdown +
remark-gfm + remark-math + rehype-katex (math common in CP).

## Run frontend (dev)

```
cd frontend
npm install
npm run dev      # http://localhost:5173, proxies /api -> :8080
npm run build    # tsc typecheck + vite build
```
Backend must be up on :8080 for live data (`docker compose up` + bootRun).

## Run EVERYTHING in Docker (one file)

`docker-compose.full.yml` (repo root) starts the whole stack — postgres, redis,
rabbitmq, minio, `codearena-api`, `codearena-judge`, and `codearena-frontend`:

```
docker compose -f docker-compose.full.yml up --build
# open http://localhost:3000   (frontend; nginx proxies /api -> codearena-api:8080)
```

Frontend container = `frontend/Dockerfile` (multi-stage: node 22 build → nginx
1.27 serve) + `frontend/nginx.conf`. nginx serves the built SPA, does SPA
fallback (`try_files … /index.html`), and reverse-proxies `/api` to the API
container so the browser stays same-origin (backend has no CORS) — same trick as
the Vite dev proxy. nginx uses Docker DNS (127.0.0.11) + a variable upstream so
it boots even before the API is up (returns 502 until ready). Ports: frontend
3000, api 8080, judge 8081, rabbit UI 15672, minio console 9001.
(`docker-compose.prod.yml` was deleted — superseded by docker-compose.full.yml.)

## Judge & build facts (critical)

- **DooD**: the containerized judge drives the HOST docker daemon via
  /var/run/docker.sock, so sandbox bind sources resolve on the HOST filesystem.
  `JUDGE_WORK_DIR` (env) must be bind-mounted at the SAME absolute path on host
  and judge container (`/tmp/codearena-judge` in compose). Unset = system temp,
  correct only when the judge runs directly on the host.
- Judge container runs as **root** (non-root can't access the mounted socket).
- Test input is fed via file + `sh -c '… < /sandbox/input.txt'` redirect, NOT
  stdin attach (docker-java attach has no reliable EOF).
- Test-case MinIO keys are deterministic: `testcases/{pid}/{00000}/input.txt` +
  `output.txt` — the judge pairs sorted keys, so naming must stay sort-stable.
- Compile and run are separate containers sharing the per-submission bind dir.
- Sandbox hardening (every run): `--network none`, `--cap-drop ALL`,
  `no-new-privileges`, `--pids-limit 128` (fork-bomb guard), captured output
  bounded to 1 MB/stream (flood guard), memory/CPU/time limits.
- `time_used_ms` is real wall-clock per run (max across tests); `ImagePrePuller`
  warms all sandbox images on startup (background daemon thread).
- `JUDGING`: the worker emits a JUDGING progress signal on pickup; the API's
  JudgeResultListener applies it only while the submission is still PENDING
  (guarded so a redelivered JUDGING can't regress a final verdict).
- Backend Dockerfiles build with `gradle:8.10.2-jdk21-alpine` (no wrapper
  download — services.gradle.org is flaky from this network).
- Sandbox images: gcc:13, eclipse-temurin:21-jdk-alpine (java),
  python:3.12-slim, node:20-slim, golang:1.22-alpine, rust:1.77-slim, and the
  custom codearena-kotlin:21 (build: `docker build -t codearena-kotlin:21
  codearena-judge/sandbox-images/kotlin`). Pre-pull or first submission stalls.
- Compile step uses a separate generous budget (1 GB / 60s), independent of the
  problem's run limits — compilers (kotlinc/rustc/g++) need far more than 256 MB.
- Rating engine: RatingService applies Codeforces-style deltas when a rated
  contest ends (RatingScheduler polls; admin POST /contests/{id}/rate triggers
  manually); idempotent via contests.ratings_applied; history in rating_changes.
- Auth errors: unauthenticated → 401, authenticated-but-forbidden → 403 (custom
  AuthenticationEntryPoint + AccessDeniedHandler in SecurityConfig).
- Known gaps: verdict/standings are poll-based (no WebSocket push, by design);
  no plagiarism detection.

## Frontend TODO / future

- Contests, blogs, leaderboard, full profile editing pages.
- Consider Monaco/CodeMirror editor upgrade.
- Verify the frontend's auto-refresh-on-401 interceptor against the live
  backend (the /auth/refresh endpoint itself is tested and works).

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
