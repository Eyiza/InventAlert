# Identity Service

Handles company registration, user management, authentication, and JWT issuance for the InventAlert platform. Runs on port **8081**.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 |
| Maven | 3.9+ (or use the included `./mvnw` wrapper) |
| Docker | Required for infrastructure and integration tests |

---

## Infrastructure required

Start MySQL, Kafka, and Zookeeper before running this service:

```bash
# From the project root
docker compose up
```

This starts:
- **MySQL** on `localhost:3308`
- **Kafka** on `localhost:9092`
- **Redis** on `localhost:6379` (used by the notification service, harmless to have running)

---

## Configuration

The service reads from `src/main/resources/env.properties` when running locally. Copy `.env.example` from the project root and fill in the values, then mirror them into `env.properties`:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3308/inventalert_identity
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

JWT_SECRET=your_secret_min_32_chars

SUPER_ADMIN_EMAIL=admin@example.com
SUPER_ADMIN_PASSWORD=SecurePassword1!
SUPER_ADMIN_ID=superadmin-fixed-uuid-0001

SMTP_HOST=sandbox.smtp.mailtrap.io
SMTP_PORT=2525
SMTP_USERNAME=your_mailtrap_user
SMTP_PASSWORD=your_mailtrap_password
MAIL_FROM=noreply@inventalert.com

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

The database (`inventalert_identity`) is created automatically by Flyway on first boot.

---

## Running

```bash
cd identityService
./mvnw spring-boot:run
```

Service starts at `http://localhost:8081`.  
Swagger UI: `http://localhost:8081/swagger-ui.html`

---

## Testing

```bash
cd identityService
./mvnw test
```

**Test breakdown:**

| Type | Location | Notes |
|------|----------|-------|
| Unit tests | `src/test/.../service/`, `security/`, `controller/` | No Docker required; dependencies are mocked |
| Integration tests | `src/test/.../integration/`, `kafka/` | Spin up real MySQL + Kafka via **Testcontainers** — Docker must be running |

Integration tests include:
- `AuthPasswordResetIT` — full reset-token flow against a real DB
- `UserControllerIT` — role-based access checks end-to-end
- `SuspendedCompanyFilterIT` — verifies suspended companies are blocked at the filter level
- `SuperAdminControllerIT` — super-admin endpoints
- `CompanyEventProducerIT` — Kafka message publishing

To run only unit tests (no Docker):

```bash
./mvnw test -Dgroups=unit
```

To run only integration tests:

```bash
./mvnw test -Dgroups=integration
```

---

## Kafka events published

| Topic | Trigger |
|-------|---------|
| `company.created` | New company registration |
| `company.suspended` | Company suspension |

The inventory service consumes `company.created` to provision a new per-company database.

---

## Key endpoints

| Method | Path | Role |
|--------|------|------|
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/forgot-password` | Public |
| `POST` | `/api/auth/reset-password` | Public |
| `GET` | `/api/users` | ADMIN, MANAGER |
| `POST` | `/api/users` | ADMIN, MANAGER |
| `PATCH` | `/api/users/{id}/role` | ADMIN, MANAGER |
| `PATCH` | `/api/users/{id}/deactivate` | ADMIN, MANAGER |
| `PATCH` | `/api/users/{id}/reactivate` | ADMIN, MANAGER |
| `GET` | `/api/companies` | SUPER_ADMIN |
| `PATCH` | `/api/companies/{id}/suspend` | SUPER_ADMIN |

> **Role note:** MANAGER can create users and change roles, but cannot assign or act on ADMIN-role users. Those actions require ADMIN.
