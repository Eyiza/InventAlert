# V1 Delivered — Honest Codebase Inventory

> This document reflects what is **actually shipped and in the codebase** as of the v1 production branch.  
> It does not include aspirational features or roadmap items.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend language | Java | 25 |
| Backend framework | Spring Boot | 4.0.6 |
| Primary database | MySQL | 8 |
| Analytics database | ClickHouse | 24.3 |
| Cache + messaging | Redis | 7-alpine |
| Event streaming | Apache Kafka | 7.6.0 (Confluent) |
| Frontend framework | React | 19.2.5 |
| Frontend build | Vite | 8.0.10 |
| State management | Redux Toolkit | 2.11.2 |
| Styling | Tailwind CSS | 4.2.2 |
| Charts | Recharts | 3.8.1 |
| WebSocket | STOMP + SockJS | 7.3.0 / 1.6.1 |
| API Gateway | Nginx | 1.25-alpine |
| Containerization | Docker + Docker Compose | — |
| Orchestration | Kubernetes (Amazon EKS) | eu-west-1 |
| Container registry | Amazon ECR | — |
| Frontend hosting | Amazon S3 | — |
| CI/CD | GitHub Actions | — |
| Monitoring | Prometheus + Grafana | — |

---

## Microservices Architecture

### 1. Identity Service (port 8081)
Handles all authentication, authorization, and company/user management.

**What is built:**
- Company self-registration (multi-tenant SaaS model)
- JWT-based authentication (RS256) shared across all services
- 4 user roles: ADMIN, MANAGER, WAREHOUSE_STAFF, PROCUREMENT_OFFICER
- Super-admin login (platform-wide, outside company scope)
- Password reset via email (expiring token link)
- Warehouse-to-user assignment (many-to-many)
- Company suspension/reactivation
- Complaint/support ticket tracking
- 11 Flyway database migrations

**Known issue in v1:** Procurement Officers are assigned company-wide in the data model, not per-warehouse.

---

### 2. Inventory Service (port 8082)
Core business logic. Largest service (105 Java files).

**What is built:**
- **Multi-tenancy:** Per-company MySQL schema (`company_<companyId>`) — complete data isolation between tenants
- **Warehouse CRUD:** Full create/read/update/delete with Google Maps Places autocomplete and lat/lng storage
- **Product catalogue:** SKU, unit of measure, per-warehouse configurable thresholds, bulk CSV import with validation
- **Stock movements (4 types):**
  - INTAKE — goods received
  - OUTBOUND_SALE — goods sold
  - TRANSFER_IN — received from another warehouse
  - TRANSFER_OUT — dispatched to another warehouse
- **Full audit trail:** Every movement is a permanent record
- **Negative stock prevention:** Guard on every outbound mutation; optimistic locking with `@Retryable` (3 attempts)
- **Velocity tracking:** Calculates units/day sold and estimated days until stockout based on movement history
- **Transfer suggestion engine:**
  1. Stock falls below configured threshold
  2. System checks for existing open alert (no duplicate alerts)
  3. Queries nearest warehouse with surplus using Google Maps Distance Matrix API (Haversine fallback if Maps unavailable)
  4. If surplus warehouse found within configurable distance cap (default 200 km): creates a TRANSFER_SUGGESTION
  5. If no surplus found: creates a RESTOCK_ALERT for procurement
- **Transfer lifecycle:** SUGGESTED → APPROVED → IN_TRANSIT → COMPLETED (or REJECTED / DELIVERY_REJECTED)
- **Restock alerts:** Status lifecycle OPEN → RESOLVED
- **Reconciliation workflow:**
  - Warehouse staff submits discrepancy (physical count vs. system count)
  - Manager reviews and approves or rejects
  - Approval adjusts stock level and appends to audit trail
  - Self-approval is prevented
- **Async processing:** Thread pool (core 5, max 20 threads)
- **Spring caching:** Applied to frequently read data
- **11 per-company Flyway migrations**

**Known issue in v1:** Transfer suggestion approval flow is inverted — source warehouse sees the suggestion before destination manager approves. Should be destination manager first.

---

### 3. Notification Service (port 8083)
Event-driven, real-time notification delivery.

**What is built:**
- Consumes Kafka events from inventory and identity services
- Stores notifications in Redis sorted sets (key: `notifications:<userId>`, score = epoch-millis for time-ordered paging)
- Unread count tracking per user (`notifications:<userId>:unread` counter in Redis)
- Idempotent processing — deduplicates by eventId using Redis SET NX
- Configurable notification TTL (default 90 days)
- Real-time WebSocket push (STOMP over SockJS) — JWT verified on CONNECT frame
- Email delivery via SMTP with retry on failure
- REST endpoints: fetch notifications, mark as read, delete

---

### 4. Analytics Service (port 8084)
Append-only analytical event store with OLAP queries.

**What is built:**
- Consumes all Kafka topics (6 consumers)
- Idempotent ingestion into ClickHouse (checks `existsByEventId` before insert)
- 6 ClickHouse tables: company_events, stock_movement_events, alert_events, transfer_events, reconciliation_events, notification_events
- REST endpoints for aggregated data:
  - Company growth and status trends
  - Stock movement summaries and trends
  - Alert frequency and status breakdown
  - Transfer status breakdown and efficiency
  - Notification volume analytics

---

### 5. Frontend (React + Vite SPA)

**What is built:**
- Public landing page
- Auth flows: login, signup, forgot password, reset password, forced password change on first login
- **5 role-based dashboards:**
  - Admin Dashboard (ADMIN)
  - Manager Dashboard (MANAGER)
  - Staff Dashboard (WAREHOUSE_STAFF)
  - Procurement Dashboard (PROCUREMENT_OFFICER)
  - Super-Admin Portal (SUPER_ADMIN)
- Route guards (ProtectedRoute) preventing unauthorized access
- Real-time notification bell with unread count badge
- Notification drawer (live feed via WebSocket STOMP)
- Status badges for transfer/alert states
- Google Places autocomplete for warehouse address input
- Recharts-powered analytics charts
- Company logo upload via Cloudinary
- Redux Toolkit Query for all API communication (with caching)
- Custom `useNotificationSocket` hook (auto-reconnect, JWT auth on connect)

---

## Kafka Event Bus (8 Topics)

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `company.created` | identityService | inventoryService, analyticsService |
| `company.offboarded` | identityService | inventoryService, analyticsService |
| `stock.movement.created` | inventoryService | analyticsService, notificationService |
| `restock.alert.created` | inventoryService | notificationService, analyticsService |
| `transfer.event` | inventoryService | notificationService, analyticsService |
| `reconciliation.event` | inventoryService | analyticsService |
| `notification.event` | notificationService | analyticsService |
| `password.reset.requested` | identityService | notificationService |

---

## Infrastructure Delivered

### Local Development
- Docker Compose file running: MySQL, Redis, Kafka, ClickHouse, all 4 services, Nginx
- Optional monitoring profile: Prometheus + Grafana

### Production (Amazon EKS)
- Kubernetes manifests for all services + infrastructure
- StatefulSets for MySQL, Redis, Kafka, ClickHouse
- Deployments for all 4 services + Nginx
- Kubernetes Ingress for external traffic
- Dedicated `inventalert-prod` namespace
- Secrets management via Kubernetes Secrets

### CI/CD Pipeline (GitHub Actions)
- Trigger: `main` branch (CI) + `production` branch (deploy)
- CI: build JARs, run unit tests on all 4 services
- Deploy: build → ECR push (SHA-tagged) → `kubectl apply` → EKS rollout
- Automatic rollback if rollout fails or pods crash
- Frontend: `npm ci && npm run build` → S3 sync
- Secrets injected at build time (Google Maps API key, Cloudinary, SMTP)

### Monitoring
- Prometheus scraping all 4 services every 15 seconds
- Pre-built Grafana dashboard: service health, HTTP rates, Kafka lag, DB pool stats, JVM metrics

---

## What Is NOT In V1 (Honest)

| Feature | Status |
|---------|--------|
| Machine learning / AI demand forecasting | Not built — all alerts are rule-based threshold logic |
| Batch/lot tracking with expiry dates | Not built |
| Price intelligence (inflation/market events) | Not built |
| Barcode scanning | Not built |
| WhatsApp notifications | Not built |
| Local language support (Igbo, Yoruba, Hausa) | Not built |
| POS system integration | Not built |
| Cursor-based pagination on movements | Not built — missing from v1 |
| Purchase order audit trail | Partial — basic movement audit exists, dedicated PO audit not built |
| Destination-manager-first transfer approval | Bug — currently inverted |
| Warehouse-scoped Procurement Officers | Bug — currently company-scoped |
| Alert filtering by warehouse | Not built |
| Race condition handling beyond optimistic locking | Handled at DB level (optimistic lock + retry) but not documented |
| NDPA compliance documentation | Not written |

---

## V1 Feature Checklist

| Feature | Shipped |
|---------|---------|
| Multi-tenant schema isolation | ✅ |
| Company self-registration | ✅ |
| JWT authentication (RS256) | ✅ |
| 4 user roles with RBAC | ✅ |
| Super-admin portal | ✅ |
| Password reset via email | ✅ |
| Warehouse CRUD + Google Maps | ✅ |
| Product catalogue + CSV import | ✅ |
| 4 stock movement types | ✅ |
| Full audit trail | ✅ |
| Negative stock prevention + optimistic locking | ✅ |
| Stock velocity (units/day, days to empty) | ✅ |
| Intelligent transfer suggestions (distance-based) | ✅ (flow bug present) |
| Restock alerts | ✅ |
| Transfer lifecycle (5 statuses) | ✅ |
| Reconciliation workflow + anti-self-approval | ✅ |
| Real-time WebSocket notifications | ✅ |
| Email notifications | ✅ |
| ClickHouse analytics + charts | ✅ |
| Prometheus + Grafana monitoring | ✅ |
| Docker Compose (dev) | ✅ |
| Kubernetes (prod, Amazon EKS) | ✅ |
| GitHub Actions CI/CD | ✅ |
| React frontend with role-based dashboards | ✅ |
| Complaint/support ticket system | ✅ |
