# InventAlert

A multi-tenant, multi-warehouse inventory management platform built with a microservices architecture. Companies self-register, manage warehouses and products, track real-time stock levels, and receive automated low-stock alerts with intelligent transfer suggestions between warehouses.

---

## Architecture Overview

```
React / Vite Frontend
        │
        ▼
  Nginx API Gateway (port 80)
        │
   ┌────┴──────────────────────────┐
   │                               │
   ▼                               ▼
identityService              inventoryService
(users, auth, JWT)           (stock, alerts, transfers)
        │                               │
        └──────────┬────────────────────┘
                   │  Kafka (event bus)
           ┌───────┴────────┐
           ▼                ▼
  notificationService   analyticsService
  (Redis, WebSocket,    (ClickHouse,
   email)               reports)
```

All four services share one JWT secret, validate tokens independently, and communicate exclusively via Kafka — no direct service-to-service HTTP calls.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend services | Java 25, Spring Boot 4 |
| Frontend | React 18, Vite, Redux Toolkit Query |
| Auth | JWT (RS256 shared secret), Spring Security |
| Primary DB | MySQL 8 (per-company schema isolation) |
| Migrations | Flyway |
| Cache / Notifications | Redis (sorted sets + hashes) |
| Messaging | Apache Kafka |
| Analytics | ClickHouse |
| API Gateway | Nginx |
| Maps / Distance | Google Maps Distance Matrix API (Haversine fallback) |
| Containerisation | Docker, Docker Compose |
| CI / CD | GitHub Actions → Amazon ECR → Amazon EKS |
| Frontend hosting | Amazon S3 |
| Monitoring | Prometheus + Grafana |

---

## Services

### identityService (port 8081)
Owns all identity and access concerns.

- Company self-registration (creates default Admin user + publishes `company.created` Kafka event)
- JWT login for all roles; separate super-admin login with no company scope
- Password reset via email token
- User CRUD, role management, warehouse assignments
- Company suspension / reactivation
- Publishes `company.offboarded` on suspension

### inventoryService (port 8082)
Contains the core business logic. Each company's data lives in an isolated MySQL schema (`company_<id>`).

- Warehouse management with Google Maps address autocomplete and lat/lng coordinates
- Product catalogue with SKU, unit of measure, and per-warehouse thresholds
- Stock movements: **INTAKE**, **OUTBOUND\_SALE**, **TRANSFER\_IN**, **TRANSFER\_OUT**
- Negative stock guard on every outbound movement
- CSV bulk intake import with row-level validation
- Velocity tracking (units sold per day, days until empty)
- **Threshold check** after every outbound sale:
  - Finds the nearest surplus warehouse via Google Maps (Haversine fallback, configurable 150 km cap)
  - Creates a transfer suggestion if a viable donor exists
  - Falls back to a restock alert otherwise
- Transfer lifecycle: `SUGGESTED → APPROVED → IN_TRANSIT → COMPLETED` (or `REJECTED` / `DELIVERY_REJECTED`)
- Reconciliation workflow: staff submit discrepancy → manager approves → stock adjusts (self-approval blocked)
- Optimistic locking + `@Retryable` on all stock-mutating operations

### notificationService (port 8083)
Event-driven notification delivery.

- Consumes Kafka events from all services
- Idempotent processing (Redis SET NX on `eventId`)
- Stores notifications in Redis sorted sets (epoch-millis score = insertion order paging)
- Unread count per user maintained as a Redis counter
- Optional email delivery via SMTP (with retry)
- Live push via STOMP/WebSocket; JWT verified on CONNECT frame

### analyticsService (port 8084)
Append-only event store for reporting.

- Consumes all Kafka topics and writes to ClickHouse
- Idempotent ingestion (existsByEventId guard before every insert)
- Exposes aggregated query endpoints: stock summaries, alert trends, transfer status breakdown, company growth, notification volume

---

## Key Features

- **Multi-tenancy** — each company gets its own MySQL schema; JWT `companyId` claim routes every request to the correct tenant
- **RBAC** — four roles (Admin, Manager, Warehouse Staff, Procurement Officer) enforced at method level via `@PreAuthorize`
- **Transfer suggestions** — system finds the cheapest internal restock before escalating to procurement
- **Real-time notifications** — WebSocket push + email for alerts, transfers, and password resets
- **Analytics dashboard** — charts for stock movement history, alert frequency, transfer efficiency, and company growth
- **Reconciliation audit trail** — every stock correction is attributed to a reporter and an approver
- **Monitoring** — Prometheus metrics scraped from all services; pre-built Grafana dashboards

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) with Compose v2
- [Node.js 20+](https://nodejs.org/) and npm
- Java 25 and Maven (only for local service development)

### Environment Setup

Copy `.env.example` to `.env` in the project root and fill in every value:

```env
# MySQL
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=inventalert_identity
MYSQL_IDENTITY_USER=identity_user
MYSQL_IDENTITY_PASSWORD=identity_password
MYSQL_INVENTORY_USER=inventory_user
MYSQL_INVENTORY_PASSWORD=inventory_password

# JWT — must be identical across all 4 services (minimum 32 characters)
JWT_SECRET=your-very-secret-key-at-least-32-chars

# Super Admin — seeds the platform admin on first startup
SUPER_ADMIN_EMAIL=admin@example.com
SUPER_ADMIN_PASSWORD=StrongPassword123!
SUPER_ADMIN_ID=superadmin-fixed-uuid-0001

# ClickHouse (Analytics)
CLICKHOUSE_PASSWORD=clickhouse_password

# Email / Notifications
SMTP_HOST=sandbox.smtp.mailtrap.io
SMTP_PORT=2525
SMTP_USERNAME=your_smtp_username
SMTP_PASSWORD=your_smtp_password
MAIL_FROM=noreply@inventalert.com
NOTIFICATION_TTL_DAYS=90

# CORS + Frontend URL
CORS_ALLOWED_ORIGINS=http://localhost
FRONTEND_URL=http://localhost

# Google Maps — leave empty to use Haversine fallback for distance calculation
GOOGLE_MAPS_API_KEY=
```

Create `inventAlert-frontend/.env` for the frontend:

```env
VITE_API_BASE_URL=http://localhost
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key
VITE_CLOUDINARY_CLOUD_NAME=your_cloud_name
VITE_CLOUDINARY_UPLOAD_PRESET=your_upload_preset
```

> Both `.env` files are listed in `.gitignore` and must never be committed.

---

### Option A — Full Docker Stack (recommended)

All backend services and infrastructure run in Docker. The frontend runs locally and proxies through Nginx on port 80.

```bash
# 1. Start infrastructure (MySQL, Redis, Kafka, ClickHouse)
docker compose up -d

# 2. Wait ~30 s for health checks, then start app services + Nginx
docker compose --profile app up -d

# Rebuild after code changes
docker compose --profile app up -d --build
docker compose --profile app up -d --build identity-service   # single service

# 3. Start the frontend
cd inventAlert-frontend
npm install      # first time only
npm run dev
```

Open **http://localhost:5173** — all API calls route through Nginx at `http://localhost`.

```bash
# Tear down
docker compose --profile app down
docker compose down -v   # also removes volumes
```

---

### Option B — Local Development (Docker infra + local services)

```bash
# Terminal 1 — infrastructure only
docker compose up -d

# Terminal 2-5 — one per service
cd identityService    && ./mvnw spring-boot:run   # :8081
cd inventoryService   && ./mvnw spring-boot:run   # :8082
cd notificationService && ./mvnw spring-boot:run  # :8083
cd analyticsService   && ./mvnw spring-boot:run   # :8084

# Terminal 6 — frontend
cd inventAlert-frontend && npm run dev             # :5173
```

Set `VITE_API_BASE_URL=http://localhost` in `inventAlert-frontend/.env` (routes through Nginx) or point directly at a service port during single-service debugging.

---

### First Run Notes

- **Super Admin** — the Identity Service seeds a platform-level super admin on first startup using `SUPER_ADMIN_*` env vars. This only runs once if no companies exist.
- **Database migrations** — Flyway runs V1–V8 migrations automatically when the Identity Service starts. The Inventory Service creates per-company schemas programmatically on company registration.
- **ClickHouse tables** — initialised from `docker/clickhouse-init.sql` on first container start.
- **Seed data** — set `app.seed-data=true` in `inventoryService/src/main/resources/application.properties` to load demo companies, warehouses, products, and stock levels on startup.

---

## Service & Port Reference

| Component | URL / Port |
|---|---|
| Frontend (dev) | http://localhost:5173 |
| Nginx API Gateway | http://localhost (port 80) |
| Identity Service | http://localhost:8081 |
| Inventory Service | http://localhost:8082 |
| Notification Service | http://localhost:8083 |
| Analytics Service | http://localhost:8084 |
| MySQL | localhost:3308 |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |
| ClickHouse HTTP | localhost:8123 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

---

## API Gateway Routing (Nginx)

| Path prefix | Routed to |
|---|---|
| `/api/auth/*`, `/api/users/*`, `/api/companies/*` | identityService:8081 |
| `/api/warehouses/*`, `/api/products/*`, `/api/stock/*`, `/api/movements/*`, `/api/transfers/*`, `/api/reconciliations/*`, `/api/alerts/*` | inventoryService:8082 |
| `/api/notifications/*` | notificationService:8083 |
| `/api/analytics/*` | analyticsService:8084 |
| `/ws/*` | notificationService:8083 (WebSocket upgrade) |

---

## Kafka Topics

| Topic | Published by | Consumed by |
|---|---|---|
| `company.created` | identityService | inventoryService, analyticsService |
| `company.offboarded` | identityService | inventoryService, analyticsService |
| `stock.movement.created` | inventoryService | analyticsService |
| `restock.alert.created` | inventoryService | notificationService, analyticsService |
| `transfer.event` | inventoryService | notificationService, analyticsService |
| `reconciliation.event` | inventoryService | analyticsService |
| `notification.event` | notificationService | analyticsService |
| `password.reset.requested` | identityService | notificationService |

---

## CI / CD

Pushes to the `production` branch trigger the GitHub Actions pipeline:

1. **CI** — compiles and unit-tests all four Java services
2. **Build JARs** — Maven packages each service
3. **Docker build & push** — images tagged with the short commit SHA and pushed to Amazon ECR
4. **Deploy** — `kubectl apply` updates the EKS cluster; each deployment is pinned to the SHA tag
5. **Rollback** — automatic `kubectl rollout undo` if the rollout times out or a pod crashes
6. **Frontend** — `npm ci && npm run build` (with all `VITE_*` secrets injected) then synced to S3

---

## Running Tests

```bash
# Identity Service
cd identityService && mvn test

# Inventory Service
cd inventoryService && mvn test

# Notification Service
cd notificationService && mvn test

# Analytics Service
cd analyticsService && mvn test
```

Integration tests (AuthControllerTest, InventoryServiceApplicationTests) use **Testcontainers** and require Docker to be running. Unit tests run without Docker.
