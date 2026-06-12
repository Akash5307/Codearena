# CodeArena — Run & Deployment Guide

Complete guide for running CodeArena locally and deploying the full stack (API + judge worker + frontend) to production.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Local Development](#local-development)
  - [1. Start Infrastructure](#1-start-infrastructure)
  - [2. Run the API Server](#2-run-the-api-server)
  - [3. Run the Judge Worker](#3-run-the-judge-worker)
  - [4. Verify Everything Works](#4-verify-everything-works)
- [Running Tests](#running-tests)
- [Docker Production Deployment](#docker-production-deployment)
  - [Single-Command Deploy](#single-command-deploy)
  - [Building Images Separately](#building-images-separately)
- [Environment Variables](#environment-variables)
- [Service Ports & URLs](#service-ports--urls)
- [Logging](#logging)
- [Health Checks](#health-checks)
- [Database Migrations](#database-migrations)
- [Manual Deployment (VPS / Bare Metal)](#manual-deployment-vps--bare-metal)
- [Cloud Deployment](#cloud-deployment)
  - [AWS / GCP / Azure VM](#aws--gcp--azure-vm)
  - [Container Registries](#container-registries)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Tool             | Version   | Required For        |
|------------------|-----------|---------------------|
| Java (JDK)       | 21+       | Building & running  |
| Docker           | 24+       | Infrastructure & judge sandbox |
| Docker Compose   | v2+       | Orchestration       |
| Git              | 2.x       | Cloning the repo    |

Verify installations:

```bash
java -version       # openjdk 21.x
docker --version     # Docker 24.x+
docker compose version  # v2.x
```

---

## Local Development

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts **4 services**:

| Service    | Port(s)        | Credentials              |
|------------|----------------|--------------------------|
| PostgreSQL | 5432           | `codearena` / `changeme` |
| Redis      | 6379           | No auth                  |
| RabbitMQ   | 5672 / 15672   | `guest` / `guest`        |
| MinIO      | 9000 / 9001    | `minioadmin` / `minioadmin` |

Wait for all services to be healthy:

```bash
docker compose ps
```

All services should show `healthy` status.

### 2. Run the API Server

```bash
./gradlew :codearena-api:bootRun
```

The API starts on **http://localhost:8080**.

On first startup, Flyway automatically runs all database migrations (`V0` through `V5`).

### 3. Run the Judge Worker

Open a **separate terminal**:

```bash
./gradlew :codearena-judge:bootRun
```

The judge worker starts on **http://localhost:8081** and connects to RabbitMQ to consume judge tasks.

**Important:** The judge worker needs access to the Docker daemon to run sandboxed code. Ensure the current user has Docker permissions (`docker ps` should work without `sudo`).

### 4. Verify Everything Works

```bash
# API health check
curl http://localhost:8080/actuator/health

# Swagger UI (open in browser)
open http://localhost:8080/swagger-ui.html

# Register a test user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser","password":"password123"}'
```

### Stop Everything

```bash
# Stop infrastructure (keep data)
docker compose down

# Stop infrastructure and delete all data
docker compose down -v
```

---

## Running Tests

```bash
# Run all tests (61 tests across 10 test classes)
./gradlew :codearena-api:test

# Run with test output
./gradlew :codearena-api:test --info

# Run a specific test class
./gradlew :codearena-api:test --tests "com.codearena.user.service.AuthServiceTest"

# Full build (compile + test both modules)
./gradlew build
```

Tests use Mockito and MockMvc — no running infrastructure required.

---

## Docker Production Deployment

### Single-Command Deploy

Deploy the entire stack (infrastructure + API + judge + frontend) with one command:

```bash
docker compose -f docker-compose.full.yml up -d --build
```

This will:
1. Build the API, Judge Worker, and frontend Docker images (multi-stage builds;
   backend builds use the `gradle:8.10.2-jdk21-alpine` image so no Gradle
   distribution download is needed)
2. Start PostgreSQL, Redis, RabbitMQ, MinIO
3. Wait for infrastructure health checks to pass
4. Start the API, Judge Worker, and frontend (nginx on port 3000, reverse-proxying `/api` to the API)

### Judge worker: Docker-out-of-Docker requirements

The judge runs **inside** a container but creates sandbox containers via the
**host's** Docker daemon (`/var/run/docker.sock` mount). Two consequences:

1. **Shared work dir at identical paths.** Sandbox bind-mount sources are
   resolved by the *host* daemon, so the judge's scratch dir must exist at the
   *same absolute path* on host and in the judge container.
   `docker-compose.full.yml` handles this with
   `-v /tmp/codearena-judge:/tmp/codearena-judge` + `JUDGE_WORK_DIR=/tmp/codearena-judge`.
   If you change the path, change both sides identically.
2. **Root inside the container.** The judge container runs as root because a
   non-root user cannot access the mounted docker.sock (host docker group GID
   does not exist inside the container).

When running the judge directly on the host (no container), neither applies —
leave `JUDGE_WORK_DIR` unset and it uses the system temp dir.

### Building Images Separately

```bash
# Build API image
docker build -t codearena-api:latest -f codearena-api/Dockerfile .

# Build Judge Worker image
docker build -t codearena-judge:latest -f codearena-judge/Dockerfile .

# Build frontend image
docker build -t codearena-frontend:latest frontend/

# Run API container
docker run -d --name codearena-api \
  -p 8080:8080 \
  -e POSTGRES_HOST=your-db-host \
  -e POSTGRES_PASSWORD=your-password \
  -e REDIS_HOST=your-redis-host \
  -e RABBITMQ_HOST=your-rabbitmq-host \
  -e MINIO_URL=http://your-minio-host:9000 \
  -e JWT_SECRET=your-256-bit-secret \
  -e SPRING_PROFILES_ACTIVE=prod \
  codearena-api:latest

# Run Judge Worker container (needs Docker socket + shared work dir, see above)
docker run -d --name codearena-judge \
  -p 8081:8081 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /tmp/codearena-judge:/tmp/codearena-judge \
  -e JUDGE_WORK_DIR=/tmp/codearena-judge \
  -e RABBITMQ_HOST=your-rabbitmq-host \
  -e MINIO_URL=http://your-minio-host:9000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  codearena-judge:latest

# Run frontend (nginx serves SPA and proxies /api to the codearena-api container)
docker run -d --name codearena-frontend -p 3000:80 codearena-frontend:latest
```

---

## Environment Variables

### API Server (`codearena-api`)

| Variable             | Default              | Description                      |
|----------------------|----------------------|----------------------------------|
| `POSTGRES_HOST`      | `localhost`          | PostgreSQL hostname              |
| `POSTGRES_PORT`      | `5432`               | PostgreSQL port                  |
| `POSTGRES_DB`        | `codearena`          | Database name                    |
| `POSTGRES_USER`      | `codearena`          | Database username                |
| `POSTGRES_PASSWORD`  | `changeme`           | Database password                |
| `REDIS_HOST`         | `localhost`          | Redis hostname                   |
| `REDIS_PORT`         | `6379`               | Redis port                       |
| `RABBITMQ_HOST`      | `localhost`          | RabbitMQ hostname                |
| `RABBITMQ_PORT`      | `5672`               | RabbitMQ port                    |
| `RABBITMQ_USER`      | `guest`              | RabbitMQ username                |
| `RABBITMQ_PASSWORD`  | `guest`              | RabbitMQ password                |
| `MINIO_URL`          | `http://localhost:9000` | MinIO endpoint URL            |
| `MINIO_ACCESS_KEY`   | `minioadmin`         | MinIO access key                 |
| `MINIO_SECRET_KEY`   | `minioadmin`         | MinIO secret key                 |
| `MINIO_BUCKET`       | `codearena-testcases`| MinIO bucket name                |
| `JWT_SECRET`         | (dev default)        | JWT signing secret (min 256-bit) |
| `JWT_ACCESS_EXPIRY`  | `900000`             | Access token expiry (ms) — 15 min |
| `JWT_REFRESH_EXPIRY` | `604800000`          | Refresh token expiry (ms) — 7 days |
| `SPRING_PROFILES_ACTIVE` | `default`        | Set to `prod` for JSON logging   |

### Judge Worker (`codearena-judge`)

| Variable             | Default              | Description                      |
|----------------------|----------------------|----------------------------------|
| `RABBITMQ_HOST`      | `localhost`          | RabbitMQ hostname                |
| `RABBITMQ_PORT`      | `5672`               | RabbitMQ port                    |
| `RABBITMQ_USER`      | `guest`              | RabbitMQ username                |
| `RABBITMQ_PASSWORD`  | `guest`              | RabbitMQ password                |
| `MINIO_URL`          | `http://localhost:9000` | MinIO endpoint URL            |
| `MINIO_ACCESS_KEY`   | `minioadmin`         | MinIO access key                 |
| `MINIO_SECRET_KEY`   | `minioadmin`         | MinIO secret key                 |
| `MINIO_BUCKET`       | `codearena-testcases`| MinIO bucket name                |
| `JUDGE_WORK_DIR`     | (system temp)        | Sandbox scratch dir. **Required when the judge runs in Docker** — must be bind-mounted at the same absolute path on host and container |

---

## Service Ports & URLs

| Service         | Port  | URL                                       |
|-----------------|-------|-------------------------------------------|
| Frontend        | 3000  | http://localhost:3000 (full-stack compose) |
| API             | 8080  | http://localhost:8080                      |
| Swagger UI      | 8080  | http://localhost:8080/swagger-ui.html      |
| API Docs (JSON) | 8080  | http://localhost:8080/api-docs             |
| Health Check    | 8080  | http://localhost:8080/actuator/health      |
| Judge Worker    | 8081  | http://localhost:8081                      |
| PostgreSQL      | 5432  | `jdbc:postgresql://localhost:5432/codearena` |
| Redis           | 6379  | `redis://localhost:6379`                   |
| RabbitMQ        | 5672  | `amqp://localhost:5672`                    |
| RabbitMQ Mgmt   | 15672 | http://localhost:15672                     |
| MinIO API       | 9000  | http://localhost:9000                      |
| MinIO Console   | 9001  | http://localhost:9001                      |

---

## Logging

### Development (default profile)

Console output with correlation IDs:

```
14:30:15.123 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.c.u.s.AuthService - User registered: testuser
```

### Production (`prod` profile)

Structured JSON logs (compatible with ELK, Datadog, CloudWatch):

```json
{"@timestamp":"2026-02-15T14:30:15.123","level":"INFO","logger":"c.c.u.s.AuthService","message":"User registered: testuser","correlationId":"a1b2c3d4"}
```

Activate production logging:

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew :codearena-api:bootRun
```

### Correlation IDs

Every request gets a correlation ID (via `X-Correlation-ID` header). Pass your own or let the server generate one. The ID is included in all log entries and returned in the response header.

---

## Health Checks

```bash
# Basic health
curl http://localhost:8080/actuator/health

# Response:
# {"status":"UP"}
```

The health endpoint checks connectivity to PostgreSQL, Redis, and RabbitMQ.

---

## Database Migrations

Flyway runs automatically on startup. Migrations are located at:

```
codearena-api/src/main/resources/db/migration/
├── V0__init_schema.sql        # Baseline
├── V1__create_users.sql       # Users table + indexes
├── V2__create_problems.sql    # Problems, test_cases, tags
├── V3__create_contests.sql    # Contests, registrations
├── V4__create_submissions.sql # Submissions + composite indexes
└── V5__create_blogs.sql       # Blog posts, comments, votes
```

To check migration status:

```bash
# Connect to PostgreSQL and check Flyway history
docker exec -it codearena-postgres psql -U codearena -c "SELECT * FROM flyway_schema_history;"
```

---

## Manual Deployment (VPS / Bare Metal)

### 1. Install Prerequisites

```bash
# Install Java 21
sudo apt update
sudo apt install -y openjdk-21-jdk

# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

### 2. Clone and Build

```bash
git clone <your-repo-url> codearena
cd codearena
./gradlew build -x test
```

### 3. Set Up Infrastructure

```bash
docker compose up -d
```

### 4. Configure Environment

Create a `.env` file:

```bash
POSTGRES_PASSWORD=your-strong-password
JWT_SECRET=your-256-bit-secret-change-this-in-production
RABBITMQ_USER=codearena
RABBITMQ_PASSWORD=your-rabbitmq-password
MINIO_ACCESS_KEY=your-minio-access
MINIO_SECRET_KEY=your-minio-secret
SPRING_PROFILES_ACTIVE=prod
```

### 5. Run as Systemd Services

Create `/etc/systemd/system/codearena-api.service`:

```ini
[Unit]
Description=CodeArena API
After=network.target docker.service
Requires=docker.service

[Service]
User=codearena
WorkingDirectory=/opt/codearena
EnvironmentFile=/opt/codearena/.env
ExecStart=/opt/codearena/gradlew :codearena-api:bootRun
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Create `/etc/systemd/system/codearena-judge.service`:

```ini
[Unit]
Description=CodeArena Judge Worker
After=network.target docker.service
Requires=docker.service

[Service]
User=codearena
WorkingDirectory=/opt/codearena
EnvironmentFile=/opt/codearena/.env
ExecStart=/opt/codearena/gradlew :codearena-judge:bootRun
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable codearena-api codearena-judge
sudo systemctl start codearena-api codearena-judge

# Check status
sudo systemctl status codearena-api
sudo journalctl -u codearena-api -f
```

### 6. Reverse Proxy (Nginx)

```nginx
server {
    listen 80;
    server_name api.codearena.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Add HTTPS with Certbot:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.codearena.com
```

---

## Cloud Deployment

### AWS / GCP / Azure VM

1. Provision a VM (recommended: 2 vCPUs, 4GB RAM minimum)
2. Open ports: 80, 443, 8080 (or use reverse proxy)
3. Follow the [Manual Deployment](#manual-deployment-vps--bare-metal) steps above
4. For managed databases, point `POSTGRES_HOST` / `REDIS_HOST` to your managed instances (RDS, ElastiCache, etc.)

### Container Registries

Push images to a container registry for deployment:

```bash
# Build images
docker build -t codearena-api:latest -f codearena-api/Dockerfile .
docker build -t codearena-judge:latest -f codearena-judge/Dockerfile .

# Tag for registry (example: Docker Hub)
docker tag codearena-api:latest yourusername/codearena-api:latest
docker tag codearena-judge:latest yourusername/codearena-judge:latest

# Push
docker push yourusername/codearena-api:latest
docker push yourusername/codearena-judge:latest
```

Then deploy on any Docker host:

```bash
docker pull yourusername/codearena-api:latest
docker pull yourusername/codearena-judge:latest
```

### Production Checklist

- [ ] Change all default passwords (`POSTGRES_PASSWORD`, `RABBITMQ_PASSWORD`, `MINIO_SECRET_KEY`)
- [ ] Set a strong `JWT_SECRET` (at least 256-bit / 32+ characters)
- [ ] Set `SPRING_PROFILES_ACTIVE=prod` for JSON logging
- [ ] Configure HTTPS (TLS) via reverse proxy
- [ ] Set up database backups for PostgreSQL
- [ ] Configure log aggregation (ELK, Datadog, CloudWatch)
- [ ] Set up monitoring and alerting on health endpoints
- [ ] Ensure the Judge Worker has Docker socket access (containerized judge runs as root for this)
- [ ] Set `JUDGE_WORK_DIR` + matching host bind mount for a containerized judge (see DooD section)
- [x] Sandbox containers are hardened by default: `--network none`, `--cap-drop ALL`, `no-new-privileges`, `--pids-limit 128`, 1 MB output cap, memory/CPU/time limits
- [x] Sandbox images are pre-pulled automatically on judge startup (background thread). Pre-pulling on the host beforehand still avoids the very first warm-up delay: `gcc:13`, `eclipse-temurin:21-jdk-alpine`, `python:3.12-slim`, `node:20-slim`, `golang:1.22-alpine`, `rust:1.77-slim`

---

## Troubleshooting

### API won't start

```bash
# Check if PostgreSQL is running
docker compose ps postgres

# Check API logs
./gradlew :codearena-api:bootRun 2>&1 | grep ERROR

# Common fix: wait for DB to be ready
docker compose up -d && sleep 10 && ./gradlew :codearena-api:bootRun
```

### Judge Worker can't create containers

```bash
# Verify Docker socket access
docker ps

# If permission denied, add user to docker group
sudo usermod -aG docker $USER
# Then log out and back in
```

### Every submission fails CE/RE (containerized judge)

Classic Docker-out-of-Docker path mismatch: the sandbox `/sandbox` bind resolves
against the **host** filesystem. If `JUDGE_WORK_DIR` is unset (or the host bind
mount is missing / at a different path), the sandbox sees an empty dir and
compilation fails on every submission.

```bash
# These two must agree and the volume must be mounted:
docker inspect codearena-judge --format '{{json .Config.Env}}' | tr ',' '\n' | grep JUDGE_WORK_DIR
docker inspect codearena-judge --format '{{json .Mounts}}' | python3 -m json.tool | grep -A2 codearena-judge
```

### Submission stuck on PENDING

The judge is down or still pulling a language image (first submission per
language pulls it, gcc:13 is ~1.4 GB). Messages are durable — they will be
judged once the worker is back. Check `docker logs codearena-judge`.

### Flyway migration fails

```bash
# Check current migration state
docker exec -it codearena-postgres psql -U codearena -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# If stuck, repair Flyway (use with caution)
# Add to application.yml: spring.flyway.repair: true
```

### RabbitMQ connection refused

```bash
# Check RabbitMQ is healthy
docker compose ps rabbitmq

# Check management UI
open http://localhost:15672  # guest/guest

# Verify queues exist
curl -u guest:guest http://localhost:15672/api/queues
```

### Redis connection issues

```bash
# Test Redis connectivity
docker exec -it codearena-redis redis-cli ping
# Should return: PONG
```
