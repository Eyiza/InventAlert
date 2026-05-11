# Testing the InventAlert Inventory Service

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Starting the Service](#2-starting-the-service)
3. [Getting a JWT Token](#3-getting-a-jwt-token)
4. [Option A — Swagger UI](#4-option-a--swagger-ui)
5. [Option B — Postman Collection](#5-option-b--postman-collection)
6. [Full End-to-End Test Walkthrough](#6-full-end-to-end-test-walkthrough)
7. [Role Reference](#7-role-reference)
8. [Triggering Business Features](#8-triggering-business-features)
9. [Common Errors and Fixes](#9-common-errors-and-fixes)

---

## 1. Prerequisites

| Requirement | How to check |
|---|---|
| Docker Desktop running | `docker ps` — should list running containers |
| `mysql-inventory` container up | `docker ps \| grep inventalert-mysql-inventory` |
| `kafka` container up | `docker ps \| grep inventalert-kafka` |
| Identity Service running | `curl http://localhost:8081/actuator/health` → `{"status":"UP"}` |
| Inventory Service running | `curl http://localhost:8082/actuator/health` → `{"status":"UP"}` |

Start infrastructure if not already running (from the `InventAlert` root directory):

```bash
docker-compose up -d mysql-inventory kafka
```

---

## 2. Starting the Service

From the `inventoryService` directory:

```bash
mvn spring-boot:run
```

Or run `InventoryServiceApplication.java` from your IDE.

The service starts on **port 8082**. Watch the logs for:
```
Started InventoryServiceApplication in X seconds
```
Followed by Kafka consumers joining their group — this is normal and takes a few seconds.

---

## 3. Getting a JWT Token

**Every request to the Inventory Service requires a JWT** issued by the Identity Service. The token must contain a `companyId` claim — the inventory service uses it to route all database operations to the correct company schema.

### Step 1 — Register a company (if you haven't already)

```http
POST http://localhost:8081/api/auth/signup
Content-Type: application/json

{
  "companyName": "Acme Corp",
  "adminEmail": "admin@acme.com",
  "password": "password123"
}
```

Save the `token` and `companyId` from the response.

### Step 2 — Log in as different roles (for role-specific endpoints)

Create users in the Identity Service, then log in as each to get their tokens:

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "admin@acme.com",
  "password": "password123"
}
```

You will need tokens for each of these roles to test all endpoints:

| Role | What to create in Identity Service |
|---|---|
| `ADMIN` | The signup user — already available |
| `MANAGER` | `POST /api/users` with `"role": "MANAGER"` |
| `WAREHOUSE_STAFF` | `POST /api/users` with `"role": "WAREHOUSE_STAFF"` |
| `PROCUREMENT_OFFICER` | `POST /api/users` with `"role": "PROCUREMENT_OFFICER"` |

> **Note:** For non-ADMIN roles, the Identity Service requires a `warehouseId` when creating the user. Use any placeholder UUID like `warehouse-uuid-001` — the Inventory Service manages its own warehouse records.

---

## 4. Option A — Swagger UI

Swagger UI is the fastest way to explore and test endpoints interactively.

### Open Swagger

```
http://localhost:8082/swagger-ui.html
```

### Authorize

1. Click the **Authorize** button (padlock icon, top-right of the page).
2. In the **bearerAuth** field, paste your JWT token — **without** the `Bearer ` prefix.
3. Click **Authorize**, then **Close**.

All subsequent requests will include your token automatically.

### Switching Roles

Swagger only holds one token at a time. To test a MANAGER endpoint after testing an ADMIN one:

1. Click **Authorize** again.
2. Click **Logout**.
3. Paste the manager's token and click **Authorize**.

### Recommended test order in Swagger

```
Warehouses  →  Products  →  Stock  →  Movements  →  Transfers  →  Reconciliations  →  Alerts
```

---

## 5. Option B — Postman Collection

### Import

1. Open Postman.
2. Click **Import** → drag in `InventAlert_InventoryService.postman_collection.json`.

### Set tokens

1. Click the collection name in the sidebar.
2. Go to the **Variables** tab.
3. Fill in the `CURRENT VALUE` column:

| Variable | Value |
|---|---|
| `adminToken` | JWT from `POST /api/auth/login` as ADMIN |
| `managerToken` | JWT from `POST /api/auth/login` as MANAGER |
| `staffToken` | JWT from `POST /api/auth/login` as WAREHOUSE_STAFF |
| `procurementToken` | JWT from `POST /api/auth/login` as PROCUREMENT_OFFICER |

Leave `warehouseId`, `productId`, etc. empty — they are auto-populated by test scripts.

### Run the collection

Use **Collection Runner** (click the ▶ button on the collection) to run all requests in order. The test scripts will:
- Auto-save `warehouseId` after "Create Warehouse"
- Auto-save `productId` after "Create Product"
- Auto-save `transferId`, `reconciliationId`, `alertId` from list responses

Or run folders individually in sequence: **Warehouses → Products → Stock → Movements → Transfers → Reconciliations → Alerts**.

---

## 6. Full End-to-End Test Walkthrough

Follow these steps in order to exercise the entire service.

### Step 1 — Create Warehouses (ADMIN token)

```http
POST http://localhost:8082/api/warehouses
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "name": "Lagos Central",
  "address": "14 Broad Street, Lagos Island",
  "latitude": 6.4550,
  "longitude": 3.3841
}
```

Expected: `201 Created` — save the `id` as `warehouseId`.

Create a second warehouse for transfer testing:

```http
POST http://localhost:8082/api/warehouses
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "name": "Abuja North",
  "address": "Plot 5 Wuse Zone 2, Abuja",
  "latitude": 9.0765,
  "longitude": 7.3986
}
```

Save this `id` as `warehouseId2`.

---

### Step 2 — Create a Product (ADMIN token)

```http
POST http://localhost:8082/api/products
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "name": "Paracetamol 500mg",
  "sku": "PARA-500",
  "unitOfMeasure": "boxes",
  "defaultThreshold": 50
}
```

Expected: `201 Created` — save the `id` as `productId`.

---

### Step 3 — Stock the warehouses (WAREHOUSE_STAFF token)

Add stock to **Warehouse 1**:

```http
POST http://localhost:8082/api/movements
Authorization: Bearer <staffToken>
Content-Type: application/json

{
  "productId": "<productId>",
  "warehouseId": "<warehouseId>",
  "type": "INTAKE",
  "quantity": 200,
  "referenceNumber": "PO-2026-001"
}
```

Add surplus stock to **Warehouse 2** (needed for transfer suggestion):

```http
POST http://localhost:8082/api/movements
Authorization: Bearer <staffToken>
Content-Type: application/json

{
  "productId": "<productId>",
  "warehouseId": "<warehouseId2>",
  "type": "INTAKE",
  "quantity": 500,
  "referenceNumber": "PO-2026-002"
}
```

---

### Step 4 — Verify stock levels (MANAGER token)

```http
GET http://localhost:8082/api/stock
Authorization: Bearer <managerToken>
```

Expected: `200 OK` — array of stock levels. Each entry includes `currentStock`, `threshold`, `velocityPerDay`, and `daysUntilEmpty`.

---

### Step 5 — Trigger a restock alert

Sell stock at Warehouse 1 down **below the threshold (50)**:

```http
POST http://localhost:8082/api/movements
Authorization: Bearer <staffToken>
Content-Type: application/json

{
  "productId": "<productId>",
  "warehouseId": "<warehouseId>",
  "type": "OUTBOUND_SALE",
  "quantity": 160
}
```

This leaves 40 units — below the threshold of 50. The velocity engine will raise a restock alert automatically.

Check it was created:

```http
GET http://localhost:8082/api/alerts
Authorization: Bearer <procurementToken>
```

Save the alert `id` as `alertId`.

---

### Step 6 — Action the alert (PROCUREMENT_OFFICER token)

Work through the alert lifecycle:

```http
PATCH http://localhost:8082/api/alerts/<alertId>/acknowledge
Authorization: Bearer <procurementToken>
```

```http
PATCH http://localhost:8082/api/alerts/<alertId>/order-placed
Authorization: Bearer <procurementToken>
```

After restocking (Step 3 repeated), resolve it:

```http
PATCH http://localhost:8082/api/alerts/<alertId>/resolve
Authorization: Bearer <procurementToken>
```

---

### Step 7 — Transfer suggestion lifecycle

The velocity engine may auto-generate a transfer suggestion when Warehouse 1 has low stock and Warehouse 2 has a surplus. List transfers to find it:

```http
GET http://localhost:8082/api/transfers
Authorization: Bearer <managerToken>
```

Save the transfer `id` as `transferId`, then run through the state machine:

| Step | HTTP call | Token |
|---|---|---|
| 1. Approve | `PATCH /api/transfers/<id>/approve` | MANAGER |
| 2. Dispatch | `PATCH /api/transfers/<id>/dispatch` | WAREHOUSE_STAFF |
| 3. Accept at destination | `PATCH /api/transfers/<id>/accept` | WAREHOUSE_STAFF |

Or to reject delivery instead of accepting:

```http
PATCH http://localhost:8082/api/transfers/<transferId>/reject-delivery
Authorization: Bearer <staffToken>
```

---

### Step 8 — Submit and approve a reconciliation

Submit a physical count (WAREHOUSE_STAFF):

```http
POST http://localhost:8082/api/reconciliations
Authorization: Bearer <staffToken>
Content-Type: application/json

{
  "productId": "<productId>",
  "warehouseId": "<warehouseId>",
  "physicalCount": 35,
  "reason": "Monthly cycle count"
}
```

Expected: `201 Created` with `status: PENDING` and a calculated `discrepancy`.

Approve it (MANAGER) — this adjusts the system stock to match the physical count:

```http
PATCH http://localhost:8082/api/reconciliations/<reconciliationId>/approve
Authorization: Bearer <managerToken>
```

---

## 7. Role Reference

| Endpoint group | ADMIN | MANAGER | WAREHOUSE_STAFF | PROCUREMENT_OFFICER |
|---|:---:|:---:|:---:|:---:|
| Create / update / deactivate warehouse | ✅ | ❌ | ❌ | ❌ |
| List warehouses | ✅ | ✅ | ❌ | ❌ |
| Create / update product | ✅ | ❌ | ❌ | ❌ |
| List products | ✅ | ✅ | ✅ | ✅ |
| Set threshold | ✅ | ❌ | ❌ | ❌ |
| View stock levels | ✅ | ✅ | ✅ (own warehouse) | ❌ |
| Record movement / CSV import | ❌ | ❌ | ✅ | ❌ |
| List movements | ❌ | ✅ | ❌ | ❌ |
| Approve / reject transfer | ❌ | ✅ | ❌ | ❌ |
| Dispatch / accept / reject-delivery | ❌ | ❌ | ✅ | ❌ |
| Submit reconciliation | ❌ | ❌ | ✅ | ❌ |
| Approve / reject reconciliation | ❌ | ✅ | ❌ | ❌ |
| List & action alerts | ❌ | ✅ | ❌ | ✅ |

---

## 8. Triggering Business Features

### Restock Alert
Automatically raised when an `OUTBOUND_SALE` movement brings stock **below the effective threshold** (warehouse-level override takes precedence over product default). Check `GET /api/alerts` after any outbound movement that crosses the threshold.

### Transfer Suggestion
Automatically generated by the velocity engine when:
- A warehouse's stock is below threshold, **and**
- Another warehouse of the same company has a surplus of the same product

The suggestion includes `distanceKm` — calculated via Google Maps API if `GOOGLE_MAPS_API_KEY` is set in `env.properties`, otherwise falls back to the Haversine formula (`distanceSource: HAVERSINE`).

### Velocity Calculation (`velocityPerDay` / `daysUntilEmpty`)
Recalculated after every `OUTBOUND_SALE` movement. Visible on `GET /api/stock`. Requires at least one outbound sale to produce a non-zero value.

### CSV Bulk Import

**Products CSV** (`POST /api/products/import`):
```csv
name,sku,unitOfMeasure,defaultThreshold
Ibuprofen 200mg,IBU-200,boxes,30
Amoxicillin 500mg,AMOX-500,strips,20
```

**Intake CSV** (`POST /api/movements/import/{warehouseId}`):
```csv
productId,quantity,referenceNumber
<productId>,100,PO-BULK-001
<productId2>,50,PO-BULK-002
```

Rows that fail validation are reported in the response without aborting the batch.

---

## 9. Common Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `401 Unauthorized` | Missing or expired JWT | Re-login via Identity Service and paste the new token |
| `403 Forbidden` | Wrong role for this endpoint | Switch to the correct token (see [Role Reference](#7-role-reference)) |
| `404 Not Found` | Wrong `productId` or `warehouseId` | List the resources first to get valid IDs |
| `409 Conflict` on reconciliation | Reconciliation is already APPROVED or REJECTED | Only PENDING reconciliations can be actioned |
| `409 Conflict` on transfer approve | Transfer is not in PENDING state | Check current status via `GET /api/transfers` |
| `400` on movement | `quantity` is 0 or negative, or `type` is invalid | Use `INTAKE` or `OUTBOUND_SALE`; quantity must be ≥ 1 |
| No alerts appearing | Stock did not cross the threshold | Check `GET /api/stock` — ensure `currentStock < threshold` after the sale |
| No transfers appearing | Velocity engine has not run yet, or no surplus warehouse exists | Record more outbound sales and ensure Warehouse 2 has surplus stock |
| `companyId not routed` / empty responses | JWT does not contain a `companyId` claim | Use a token from a non-SuperAdmin login; SuperAdmin tokens have no `companyId` |
