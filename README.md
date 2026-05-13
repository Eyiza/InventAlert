***InventAlert***  
A multi-warehouse company manages stock levels across locations. Warehouse staff can record stock intake and dispatch events for products. The system tracks current stock levels per product per warehouse. When stock for any product falls below a configurable reorder threshold, the system automatically raises a restock alert and notifies the relevant procurement officer. Managers can view current stock levels, movement history, and pending alerts. Admins configure products, warehouses, thresholds, and user assignments. The system must handle stock adjustments and reconciliations without creating negative stock levels.  
Actors: WarehouseStaff, ProcurementOfficer, Manager, Admin  
Core Entities: Product, Warehouse, StockLevel, StockMovement, RestockAlert, User  
Natural Kafka Trigger: Stock falls below threshold RestockAlertCreated event notifies →→procurement officer

***Functional Requirements***

1. A company shall be able to self-register their inventory system which creates a default admin user.  
2. Registered users shall be able to log in with email and password  
3. An Admin shall be able to create user accounts within their tenant and assign roles: Admin, Manager, WarehouseStaff, ProcurementOfficer.  
4. An Admin shall be able to assign WarehouseStaff users to one or more specific warehouses.  
5. An Admin should be able to manage warehouses, manage products and update user roles.  
6. WarehouseStaff shall be able to record stock intake (goods received) specifying product, quantity, and reference number. Stock level shall increase immediately.  
7. WarehouseStaff shall be able to record outbound sales (goods leaving the company) specifying product and quantity. The system shall reject the movement if it would result in negative stock.  
8. When stock falls below the reorder threshold, the system should send an alert.  
9. WarehouseStaff shall be able to submit a reconciliation request when physical count differs from system count, specifying the discrepancy and a reason.  
10. A Manager shall be able to approve or reject reconciliation requests. Stock levels shall only adjust upon approval. All reconciliations shall retain a full audit trail.

***Non functional Requirements***

1. Security on all actions.  
2. Reconciliation adjustments shall require manager approval before affecting stock. No staff member can self-approve.  
3. Performance \- less than 200ms latency  
   

***Actors***

- Admin  
- Manager  
- Warehouse Staff  
- Procurement Officer  
- Invent Alert Application

***Entities***

- Tenant  
- User  
- WarehouseAssignment  
- Warehouse  
- Product  
- StockLevel  
- StockMovement  
- RestockAlert  
- Reconciliation  
- Notification

***Design Diagrams:***

- ERD  
- Use case  
- Activity   
- UML  
- Sequence

***Additional:***

- Batch upload products  
- Platform customer care can be alerted of issues with the platform like the DeskFlow 

Future Core Entities:

- TransferSuggestion  
- DailyMovementSummary  
- AlertFrequency  
- TransferEfficiency

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (with Compose v2)
- [Node.js 20+](https://nodejs.org/) and npm (for the frontend)
- Java 21+ and Maven (only needed for Option B — local service dev)

---

### Environment Setup

Create a `.env` file in the project root by copying `.env.example` and filling in every value:

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

# Super Admin — used only on first startup to seed the platform admin account
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

# CORS + Frontend URL (used by backend services)
CORS_ALLOWED_ORIGINS=http://localhost
FRONTEND_URL=http://localhost

# Optional — leave empty to use Haversine fallback for distance calculation
GOOGLE_MAPS_API_KEY=
```

Create `inventAlert-frontend/.env` for the frontend:

```env
VITE_API_BASE_URL=http://localhost
VITE_CLOUDINARY_CLOUD_NAME=your_cloud_name
VITE_CLOUDINARY_UPLOAD_PRESET=your_upload_preset
VITE_CLOUDINARY_API_KEY=your_api_key
VITE_CLOUDINARY_API_SECRET=your_api_secret
VITE_GOOGLE_MAPS_API_KEY=
```

> **Note:** Both `.env` files are listed in `.gitignore` and must never be committed.

---

### Option A — Full Docker Stack (recommended for end-to-end testing)

All backend services and infrastructure run inside Docker. The frontend runs locally and connects through Nginx on port 80.

```bash
# 1. Start infrastructure (MySQL, Redis, Kafka, ClickHouse)
docker compose up -d

# 2. Wait ~30 seconds for health checks, then start app services + Nginx
docker compose --profile app up -d

# To rebuild services after code changes:
docker compose --profile app up -d --build
docker compose --profile app up -d --build identity-service # rebuild just one service if needed
docker compose --profile app up --build identity-service inventory-service notification-service # rebuild multiple services

# 3. Start the frontend (not included in Docker)
cd inventAlert-frontend
npm install        # first time only
npm run dev
```

Open the app at **http://localhost:5173** — all API calls route through Nginx at `http://localhost`.

To stop everything:

```bash
docker compose --profile app down
# also stop infrastructure if you want: docker compose down -v
```

---

### Option B — Local Development (Docker infra + local services)

Run infrastructure in Docker but each Spring Boot service locally, useful for faster iteration with hot-reload.

```bash
# Terminal 1 — Infrastructure only
docker compose up -d

# Terminal 2 — Identity Service (port 8081)
cd identityService && ./mvnw spring-boot:run

# Terminal 3 — Inventory Service (port 8082)
cd inventoryService && ./mvnw spring-boot:run

# Terminal 4 — Notification Service (port 8083)
cd notificationService && ./mvnw spring-boot:run

# Terminal 5 — Analytics Service (port 8084)
cd analyticsService && ./mvnw spring-boot:run

# Terminal 6 — Frontend (port 5173)
cd inventAlert-frontend && npm run dev
```

In this mode, set `VITE_API_BASE_URL=http://localhost:8081` in `inventAlert-frontend/.env` (or leave it unset — the fallback is `http://localhost:8081`).

---

### Service & Port Reference

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

---

### API Gateway Routing (Nginx)

All frontend requests go through `http://localhost`. Nginx routes by path prefix:

| Path Prefix | Routed To |
|---|---|
| `/api/auth/*`, `/api/users/*`, `/api/companies/*` | identity-service:8081 |
| `/api/warehouses/*`, `/api/products/*`, `/api/stock/*`, `/api/movements/*`, `/api/transfers/*`, `/api/reconciliations/*`, `/api/alerts/*` | inventory-service:8082 |
| `/api/notifications/*` | notification-service:8083 |
| `/api/analytics/*` | analytics-service:8084 |
| `/ws/*` | notification-service:8083 (WebSocket) |

---

### First Run Notes

- **Super Admin:** On first startup the Identity Service seeds a platform-level super admin using `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD`, and `SUPER_ADMIN_ID`. This only runs once if no companies exist.
- **Database migrations:** The Identity Service runs Flyway migrations (V1–V8) automatically on startup. The Inventory Service creates per-company schemas programmatically when a company is registered.
- **ClickHouse tables:** Initialized from `docker/clickhouse-init.sql` on first container start.