# Service Breakdown

## Identity Service (port 8081)

**Responsibility:** Authentication, user management, company lifecycle, JWT issuance.

**Data store:** MySQL — `inventalert_identity` schema with Flyway-managed migrations (V1–V8).

**Key domain objects:**
- `Company` — the tenant; has a status (ACTIVE, SUSPENDED, OFFBOARDED)
- `User` — belongs to a company; has a role (ADMIN, MANAGER, WAREHOUSE_STAFF, PROCUREMENT_OFFICER)
- `WarehouseAssignment` — maps a user to a warehouse ID (reference to Inventory Service)
- `PasswordResetToken` — time-limited token for forgot-password flow

**External interactions:**
- Publishes `company.created` and `company.offboarded` to Kafka on signup/offboarding
- Publishes `password.reset.requested` to Kafka on forgot-password

**Security model:**
- Passwords hashed with BCrypt
- JWT signed with HMAC-SHA256, carries `userId`, `companyId`, `role`, `warehouseId`
- Spring Security `@PreAuthorize` on every protected endpoint
- `SuspendedCompanyFilter` blocks all requests from suspended companies at the filter level

---

## Inventory Service (port 8082)

**Responsibility:** Multi-tenant stock management — warehouses, products, stock levels, movements, alerts, transfers, reconciliations.

**Data store:** MySQL — one schema per company (`tenant_{companyId}`), created dynamically on `company.created` event. Schema is shared-nothing at the DB level.

**Key domain objects:**
- `Warehouse` — physical location with GPS coordinates
- `Product` — SKU, unit of measure, default reorder threshold
- `StockLevel` — current stock, per-warehouse threshold, 7-day velocity
- `StockMovement` — INTAKE, OUTBOUND_SALE, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT
- `RestockAlert` — lifecycle: OPEN → ACKNOWLEDGED → ORDER_PLACED → RESOLVED
- `TransferSuggestion` — lifecycle: SUGGESTED → APPROVED → IN_TRANSIT → COMPLETED
- `Reconciliation` — lifecycle: PENDING_APPROVAL → APPROVED / REJECTED

**Business logic highlights:**
- After every outbound sale: velocity is recalculated, threshold is checked. If stock < threshold, the system checks for surplus at other warehouses and either suggests a transfer or raises a restock alert.
- Negative stock is blocked at the service layer — `InsufficientStockException` is thrown before the movement is saved.
- `getOrCreate` on StockLevel handles concurrent inserts with `DataIntegrityViolationException` retry.
- Transfer rejection and delivery rejection both auto-escalate to a restock alert.

**External interactions:**
- Consumes `company.created` (schema provisioning) and `company.offboarded` (archiving)
- Publishes stock, alert, transfer, and reconciliation events to Kafka

---

## Notification Service (port 8083)

**Responsibility:** Deliver in-app and email notifications; real-time push via WebSocket.

**Data store:** Redis — notifications stored as JSON with a configurable TTL (default 90 days). No SQL schema.

**Key domain objects:**
- `Notification` — stored in Redis with a composite key `{companyId}:{notificationId}`

**Delivery channels:**
1. **In-app (REST):** `GET /api/notifications` for polling, `PATCH /{id}/read` for marking read.
2. **Real-time (WebSocket):** STOMP over WebSocket at `/ws`. Users subscribe to `/topic/notifications/{userId}`.
3. **Email:** SMTP via `spring-boot-starter-mail`. Mailtrap in dev, production SMTP in prod.

**Kafka consumers:** Listens on `restock.alert.created`, `transfer.*`, `reconciliation.*`, `password.reset.requested`, and `notification.events`.

---

## Analytics Service (port 8084)

**Responsibility:** Event ingestion from Kafka into ClickHouse; OLAP queries for the frontend dashboards.

**Data store:** ClickHouse — `inventalert_analytics` database. Tables use `MergeTree` with monthly partitioning (`toYYYYMM(event_time)`).

**Tables:**
- `company_events` — onboarding/offboarding events
- `stock_movement_events` — all intake and outbound events
- `alert_events` — restock alert creation events
- `transfer_events` — all transfer lifecycle events
- `reconciliation_events` — reconciliation submission events
- `notification_events` — notification delivery events

**Idempotency:** All consumers check `existsByEventId` before inserting. Duplicate Kafka deliveries are silently dropped.

**API surface:** Read-only. No writes through the REST API — all data arrives via Kafka.

**Query capabilities:**
- Time-range filtering (ISO 8601, defaults to last 30 days, max 1 year)
- Warehouse-scoped queries for MANAGER role
- Company-scoped queries for ADMIN
- Platform-wide queries for SUPER_ADMIN
