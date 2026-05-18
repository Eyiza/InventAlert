# API Reference

## Swagger UI — Interactive Docs

Each service ships its own Swagger UI. Start the stack and open these URLs in your browser:

| Service | Direct URL | Via Gateway (port 8080) |
|---|---|---|
| **Identity** | http://localhost:8081/swagger-ui.html | http://localhost:8080/identity/swagger-ui.html |
| **Inventory** | http://localhost:8082/swagger-ui.html | http://localhost:8080/inventory/swagger-ui.html |
| **Notification** | http://localhost:8083/swagger-ui.html | http://localhost:8080/notification/swagger-ui.html |
| **Analytics** | http://localhost:8084/swagger-ui.html | http://localhost:8080/analytics/swagger-ui.html |

### OpenAPI JSON specs

| Service | Spec URL |
|---|---|
| Identity | http://localhost:8081/v3/api-docs |
| Inventory | http://localhost:8082/v3/api-docs |
| Notification | http://localhost:8083/v3/api-docs |
| Analytics | http://localhost:8084/v3/api-docs |

---

## Authentication

All protected endpoints require a Bearer JWT in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

**How to get a token:**

1. Call `POST /api/auth/signup` to register a new company and receive a token for the ADMIN user.
2. Call `POST /api/auth/login` with `{ "email": "...", "password": "..." }` to authenticate any existing user.
3. Paste the returned `token` value into Swagger UI's **Authorize** button (top-right), or into the `Authorization` header of your API client.

**Token claims include:**
- `sub` — userId
- `companyId`
- `role` — ADMIN | MANAGER | WAREHOUSE_STAFF | PROCUREMENT_OFFICER | SUPER_ADMIN
- `warehouseId` — populated for WAREHOUSE_STAFF and PROCUREMENT_OFFICER

Tokens expire after 24 hours. Call `GET /api/auth/me` to refresh.

---

## Role Matrix

| Role | Can do |
|---|---|
| ADMIN | Full company configuration: warehouses, products, users, thresholds, reconciliation overview |
| MANAGER | Approve/reject transfers and reconciliations; view all stock and movements |
| WAREHOUSE_STAFF | Record intake/outbound, initiate transfers, dispatch/accept transfers, submit reconciliations |
| PROCUREMENT_OFFICER | View and action restock alerts, record intake |
| SUPER_ADMIN | Platform-wide management, company suspension/offboarding, platform analytics |

---

## Endpoint Overview

See [endpoints.md](endpoints.md) for the full table of all 40+ endpoints across all four services.

## WebSocket (Real-time Notifications)

See [websocket.md](websocket.md) for the STOMP connection guide.
