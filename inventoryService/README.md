# Inventory Service

Manages warehouses, products, stock levels, movements, transfers, reconciliations, and low-stock alerts for InventAlert. Runs on port **8082**.

Each company's data is fully isolated in its own database (`company_{id}`). The service creates and migrates these databases automatically — no manual DB setup needed beyond the MySQL container.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 |
| Maven | 3.9+ (or use the included `./mvnw` wrapper) |
| Docker | Required for infrastructure |

---

## Infrastructure required

Start MySQL and Kafka before running this service:

```bash
# From the project root
docker compose up
```

This starts:
- **MySQL** on `localhost:3308`
- **Kafka** on `localhost:9092`

> **Note:** The identity service must be running too. The inventory service provisions a new `company_{id}` database when it receives a `company.created` Kafka event from the identity service. Without that event, there is no database to route requests to.

---

## Configuration

The service reads from `src/main/resources/env.properties` when running locally:

```properties
# No database name — the service connects to the MySQL root and routes per-request
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3308/
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

JWT_SECRET=your_secret_min_32_chars

GOOGLE_MAPS_API_KEY=your_key_or_leave_blank

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

The `JWT_SECRET` must be **identical** to the value used in the identity service.

### Why there is no database name in the URL

The inventory service uses a multi-tenant design. When a company registers, the identity service emits a `company.created` event. This service consumes it and:

1. Creates database `company_{id}` on the MySQL server
2. Runs all Flyway migrations against that database
3. Registers a new connection pool entry for that company

Every incoming request carries a JWT. The `CompanyRoutingInterceptor` reads `companyId` from the token, stores it in a thread-local, and `CompanyRoutingDataSource` routes the JDBC connection to the correct `company_*` database. See `DATABASE_ARCHITECTURE.md` in the project root for a full explanation.

---

## Running

```bash
cd inventoryService
./mvnw spring-boot:run
```

Service starts at `http://localhost:8082`.  
Swagger UI: `http://localhost:8082/swagger-ui.html`

---

## Dev seed data

On first startup, the service provisions schemas for all 4 seeded companies and fills each with warehouses, products, stock levels, movements, transfer suggestions, and restock alerts. This is controlled by `app.seed-data=true` in `src/main/resources/env.properties`.

The seeder implements `ApplicationRunner` — Spring calls it automatically after the context loads. It provisions each company's schema first (idempotent: `CREATE DATABASE IF NOT EXISTS`), then checks `warehouseRepository.count() > 0` per company before inserting anything.

| Company | Warehouses | Products | Open alerts | Pending transfers |
|---|---|---|---|---|
| Pharmaplus Nigeria Ltd | Lagos, Abuja | 17 | 4 | 2 |
| Eko Fresh Market | Lagos, Ibadan | 16 | 3 | 3 |
| Lagos Living Furniture | Lagos Island, Lekki | 15 | 2 | 2 |
| TechZone Gadgets | Ikeja, Abuja | 17 | 2 | 2 |

To reseed from scratch, drop the `company_*` databases and restart. To disable seeding, set `app.seed-data=false` in `env.properties`.

---

## Testing

```bash
cd inventoryService
./mvnw test
```

All tests are **unit tests** — dependencies (repositories, external services) are mocked. No Docker or running infrastructure is needed.

**Test coverage includes:**

- `ProductServiceTest` — CRUD, search, stock level linking
- `WarehouseServiceTest` — warehouse management, assignment checks
- `StockLevelServiceTest` — stock reads and updates
- `MovementServiceTest` — inbound/outbound movement recording
- `TransferServiceTest` — transfer suggestion lifecycle
- `ReconciliationServiceTest` — reconciliation approval with optimistic locking
- `ThresholdCheckServiceTest` — low-stock detection, duplicate suggestion guard
- `VelocityCalculationServiceTest` — velocity recalculation with retry logic
- `RestockAlertServiceTest` — alert creation and delivery
- `GoogleMapsServiceTest` — distance calculation mocking

---

## Kafka events consumed

| Topic | Action |
|-------|--------|
| `company.created` | Provisions new `company_{id}` database and runs Flyway migrations |
| `company.suspended` | Blocks all requests for that company |

---

## Key endpoints

All endpoints require a valid JWT. The `companyId` is read from the token — it does not need to be passed separately.

| Method | Path | Role |
|--------|------|------|
| `GET` | `/api/warehouses` | Any authenticated |
| `POST` | `/api/warehouses` | ADMIN |
| `GET` | `/api/products` | Any authenticated |
| `POST` | `/api/products` | ADMIN, MANAGER |
| `PATCH` | `/api/products/{id}` | ADMIN, MANAGER |
| `GET` | `/api/stock` | Any authenticated |
| `POST` | `/api/movements` | ADMIN, MANAGER, STAFF |
| `GET` | `/api/transfers` | ADMIN, MANAGER |
| `PATCH` | `/api/transfers/{id}/approve` | ADMIN, MANAGER |
| `GET` | `/api/reconciliations` | ADMIN, MANAGER |
| `POST` | `/api/reconciliations` | ADMIN, MANAGER |
| `PATCH` | `/api/reconciliations/{id}/approve` | ADMIN |
| `GET` | `/api/alerts` | Any authenticated |
