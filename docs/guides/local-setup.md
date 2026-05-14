# Local Development Setup

## Prerequisites

| Tool | Minimum version |
|---|---|
| Docker Desktop | 4.x |
| Java | 25 (Temurin) |
| Node.js | 20 |
| Maven | 3.9 |

---

## Option A — Full Stack via Docker Compose (Recommended)

Start everything (infrastructure + all four services + frontend) with a single command:

```bash
# 1. Copy environment variables
cp .env.example .env
# Edit .env and fill in SMTP credentials (Mailtrap works for dev)

# 2. Start infrastructure (MySQL, Redis, Kafka, ClickHouse)
docker compose up -d mysql redis kafka clickhouse

# 3. Wait ~15 seconds for databases to be ready, then start services
docker compose --profile app up -d

# 4. (Optional) Start monitoring stack
docker compose --profile monitoring up -d
```

Services will be available at:

| Service | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Identity Service | http://localhost:8081 |
| Inventory Service | http://localhost:8082 |
| Notification Service | http://localhost:8083 |
| Analytics Service | http://localhost:8084 |
| Frontend (dev) | http://localhost:5173 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

---

## Option B — Run Services Locally (Faster Iteration)

Start only the infrastructure containers, then run each service on the JVM:

```bash
# Start infrastructure only
docker compose up -d mysql redis kafka clickhouse

# Run each service (in separate terminals)
cd identityService  && mvn spring-boot:run
cd inventoryService && mvn spring-boot:run
cd analyticsService && mvn spring-boot:run
cd notificationService && mvn spring-boot:run

# Start frontend
cd inventAlert-frontend
npm install
npm run dev
```

---

## First-Run Notes

1. **MySQL** initialises with two user accounts (`identity_user` and `inventory_user`) and the `inventalert_identity` schema. The Inventory Service creates per-company schemas dynamically when a company signs up.

2. **ClickHouse** initialises the `inventalert_analytics` database and all event tables from `docker/clickhouse-init.sql`.

3. **Kafka** topics are created automatically by the producers on first message.

4. **Flyway** migrations run automatically on Identity Service startup (V1 through V8).

---

## Test Users

See [SEED_USERS.md](../../SEED_USERS.md) for pre-configured credentials for each role (ADMIN, MANAGER, WAREHOUSE_STAFF, PROCUREMENT_OFFICER, SUPER_ADMIN).

---

## Running Tests

```bash
# Unit tests (no Docker required)
cd identityService && mvn test

# All tests including integration (Docker required for TestContainers)
cd inventoryService && mvn verify
```
