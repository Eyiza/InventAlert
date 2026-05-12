# Notification Service

Delivers in-app and email notifications to InventAlert users. Consumes events from Kafka, persists notifications in Redis, and pushes them to connected clients over WebSocket. Runs on port **8083**.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 |
| Maven | 3.9+ (or use the included `./mvnw` wrapper) |
| Docker | Required for infrastructure and integration tests |

> **No MySQL required.** This service uses Redis for notification storage only.

---

## Infrastructure required

Start Kafka and Redis before running this service:

```bash
# From the project root
docker compose up
```

This starts:
- **Kafka** on `localhost:9092`
- **Redis** on `localhost:6379`

---

## Configuration

The service reads from `src/main/resources/env.properties` when running locally:

```properties
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

JWT_SECRET=your_secret_min_32_chars

SMTP_HOST=sandbox.smtp.mailtrap.io
SMTP_PORT=2525
SMTP_USERNAME=your_mailtrap_user
SMTP_PASSWORD=your_mailtrap_password
MAIL_FROM=noreply@inventalert.com

NOTIFICATION_TTL_DAYS=90

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

The `JWT_SECRET` must be **identical** to the value used in the identity service (this service validates JWTs but does not issue them).

---

## Running

```bash
cd notificationService
./mvnw spring-boot:run
```

Service starts at `http://localhost:8083`.  
WebSocket endpoint: `ws://localhost:8083/ws`

---

## Testing

```bash
cd notificationService
./mvnw test
```

**Test breakdown:**

| Type | Location | Notes |
|------|----------|-------|
| Unit tests | `src/test/.../service/`, `consumer/`, `controller/`, `security/` | No Docker required; dependencies are mocked |
| Integration tests | `src/test/.../*IT.java` | Spin up real Kafka + Redis via **Testcontainers** — Docker must be running |

Integration tests include:
- `NotificationControllerIT` — REST endpoints with a real Redis store
- `NotificationEventConsumerIT` — Kafka consumer receiving events end-to-end

Unit tests include:
- `NotificationServiceTest` — persistence and retrieval logic
- `NotificationEventConsumerTest` — Kafka event deserialization and routing
- `NotificationBroadcasterTest` — WebSocket push logic
- `EmailServiceTest` — SMTP send with mocked JavaMailSender
- `WebSocketAuthInterceptorTest` — JWT validation on WebSocket handshake
- `NotificationControllerTest` — REST layer with mocked service

---

## Kafka events consumed

| Topic | Action |
|-------|--------|
| `restock.alert` | Creates low-stock notification and sends email to warehouse managers |
| `transfer.approved` | Notifies relevant users of an approved transfer |
| `reconciliation.approved` | Notifies relevant users of a completed reconciliation |

---

## Key endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET` | `/api/notifications` | Any authenticated | List notifications for the calling user |
| `PATCH` | `/api/notifications/{id}/read` | Any authenticated | Mark a notification as read |
| `DELETE` | `/api/notifications/{id}` | Any authenticated | Delete a notification |

WebSocket connection (for real-time push):

```
ws://localhost:8083/ws
```

Clients must pass a valid JWT as a query parameter or in the `Authorization` header on the WebSocket handshake. The `WebSocketAuthInterceptor` validates the token before allowing the connection.
