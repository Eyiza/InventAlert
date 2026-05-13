 InventAlert — Sequential Integration Plan

  Reading conventions

  - ✅ in API = RTK Query endpoint already defined in inventAlertApi.js
  - ❌ add to API = backend controller exists, endpoint must be added to inventAlertApi.js
  - 🔴 500 = returns 500 for seeded companies until Step 0 is fixed
  - Redux-only = slice dispatches local state, never calls the backend

  ---
  Step 0 — Fix DevDataSeeder / Kafka company.created (BLOCKER for Steps 2–5)

  Root cause: identityService's DevDataSeeder inserts company rows directly into MySQL without publishing company.created Kafka events. The
  inventoryService only provisions per-company MySQL schemas when it consumes that event. Seeded companies therefore have no schema → every
  /api/movements and /api/transfers call returns 500.

  Backend files to change:

  File: identityService/src/main/java/.../DevDataSeeder.java
  Change: Inject CompanyEventProducer; after each company row is inserted (inside the if (!companyRepo.existsById(id)) guard), call
    companyEventProducer.publishCompanyCreated(companyId, companyName, adminEmail)

  Guard the publish with the same idempotency check wrapping the insert — if the company row already exists, skip both the insert and the
  publish so docker compose up restarts are safe.

  Acceptance criteria before Step 1:
  - docker compose down -v && docker compose --profile app up — all containers healthy
  - GET /api/movements returns HTTP 200 for all 4 seeded companies
  - GET /api/transfers returns HTTP 200 for all 4 seeded companies
  - ClickHouse: SELECT count() FROM inventalert_analytics.stock_movements returns > 0 (analytics consumer received events)

  ---
  Step 1 — Admin Dashboard

  Endpoints consumed

  ┌─────────────────────────────────────────────────────────────┬───────────┬───────────────┐
  │                            Path                             │  Service  │    Status     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/companies/me                                       │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/companies/me                                     │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/users                                              │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ POST /api/users                                             │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/users/{id}/role                                  │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/users/{id}/deactivate                            │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/users/{id}/reactivate                            │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ POST /api/users/{id}/assign                                 │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/users/{id}/assignments                             │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ DELETE /api/users/{userId}/assignments/{assignmentId}       │ Identity  │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/warehouses                                         │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ POST /api/warehouses                                        │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/warehouses/{id}                                  │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/warehouses/{id}/deactivate                       │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/warehouses/{id}/activate                         │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/products                                           │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ POST /api/products                                          │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/products/{id}                                    │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ POST /api/products/import                                   │ Inventory │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/products/{id}/threshold                          │ Inventory │ ❌ add to API │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ PATCH /api/stock-levels/{productId}/{warehouseId}/threshold │ Inventory │ ❌ add to API │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/analytics/stock/summary                            │ Analytics │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/analytics/stock/movements/trend                    │ Analytics │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/analytics/transfers/summary                        │ Analytics │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/analytics/alerts/summary                           │ Analytics │ ✅ in API     │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/analytics/stock/top-products                       │ Analytics │ ❌ add to API │
  ├─────────────────────────────────────────────────────────────┼───────────┼───────────────┤
  │ GET /api/analytics/stock/movements/by-warehouse             │ Analytics │ ❌ add to API │
  └─────────────────────────────────────────────────────────────┴───────────┴───────────────┘

  Pre-check — nginx routing: Confirm nginx.conf has a location /api/analytics/ block proxying to analytics-service:8084. If missing, add it
  before touching any frontend code; all analytics calls will 502 without it.

  Files to change

  inventAlert-frontend/src/apis/inventAlertApi.js — add 4 endpoints:
  setProductDefaultThreshold   PATCH /api/products/{id}/threshold
  setStockLevelThreshold       PATCH /api/stock-levels/{productId}/{warehouseId}/threshold
  getTopProducts               GET   /api/analytics/stock/top-products?from=&to=
  getMovementsByWarehouse      GET   /api/analytics/stock/movements/by-warehouse?from=&to=
  Tag setProductDefaultThreshold and setStockLevelThreshold to invalidate Stock and Product tags so stock badges re-render on save.

  nginx.conf — add analytics proxy block if absent.

  Acceptance criteria before Step 2

  - Login as admin@pharmaplus.ng — JWT received, dashboard renders
  - Company profile loads; edit company name → PATCH /api/companies/me returns 200; name persists on refresh
  - User list shows 5 seeded users; change a role → PATCH /api/users/{id}/role 200; deactivate/reactivate cycle completes
  - Create a new warehouse → appears in list; deactivate it
  - Create a product manually + import from CSV → product list updates
  - Edit a product's default restock threshold → PATCH /api/products/{id}/threshold 200
  - All analytics charts (stock summary, movement trend, transfer summary, alert summary, top-products, by-warehouse) render real ClickHouse
  data — no hardcoded mock arrays visible in Redux DevTools

  ---
  Step 2 — Manager Dashboard

  Endpoints consumed

  ┌──────────────────────────────────────────┬──────────────┬──────────────────────────────────────────────────┐
  │                   Path                   │   Service    │                      Status                      │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/stock                           │ Inventory    │ ❌ add to API (all-warehouse view, MANAGER role) │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/stock/{warehouseId}             │ Inventory    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/movements                       │ Inventory    │ ✅ in API — 🔴 500 until Step 0                  │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/transfers                       │ Inventory    │ ✅ in API — 🔴 500 until Step 0                  │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ PATCH /api/transfers/{id}/approve        │ Inventory    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ PATCH /api/transfers/{id}/reject         │ Inventory    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/reconciliations                 │ Inventory    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ PATCH /api/reconciliations/{id}/approve  │ Inventory    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ PATCH /api/reconciliations/{id}/reject   │ Inventory    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/analytics/stock/summary         │ Analytics    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/analytics/stock/movements/trend │ Analytics    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/analytics/transfers/summary     │ Analytics    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/analytics/alerts/summary        │ Analytics    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/analytics/alerts/by-warehouse   │ Analytics    │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/analytics/notifications/summary │ Analytics    │ ❌ add to API                                    │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/notifications                   │ Notification │ ✅ in API                                        │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ GET /api/notifications/unread-count      │ Notification │ ❌ add to API                                    │
  ├──────────────────────────────────────────┼──────────────┼──────────────────────────────────────────────────┤
  │ PATCH /api/notifications/{id}/read       │ Notification │ ✅ in API                                        │
  └──────────────────────────────────────────┴──────────────┴──────────────────────────────────────────────────┘

  Files to change

  inventAlert-frontend/src/apis/inventAlertApi.js — add 3 endpoints:
  getAllStock               GET /api/stock              (tag: Stock)
  getNotificationAnalytics GET /api/analytics/notifications/summary?from=&to=
  getUnreadCount           GET /api/notifications/unread-count

  inventAlert-frontend/src/store/slices/transfersSlice.js — audit: any approve/reject reducer that writes to local state without calling the
  API mutation must be replaced with the RTK Query mutation hook in the component; the slice itself can stay for local UI state (selected row,
   modal open flag).

  inventAlert-frontend/src/store/slices/reconciliationsSlice.js — same audit: approve/reject must fire through RTK Query, not local
  dispatch(approveReconciliation(id)).

  Acceptance criteria before Step 3

  - Login as manager@pharmaplus.ng
  - Movements list loads with real records and warehouse/product filters narrow results
  - Transfers list loads; approve a PENDING transfer → status changes to APPROVED in DB; refresh confirms
  - Reject a different PENDING transfer → status becomes REJECTED
  - Reconciliations list loads; approve/reject cycle changes DB status
  - Stock overview (all warehouses) shows real stock levels — useGetAllStockQuery data visible in Redux DevTools, no mock array
  - Notification bell shows real unread count; clicking mark-as-read decrements it
  - Notification analytics chart renders

  ---
  Step 3 — Warehouse Staff Dashboard

  Endpoints consumed

  ┌───────────────────────────────────────────┬──────────────┬─────────────────────────────────┐
  │                   Path                    │   Service    │             Status              │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ GET /api/stock/{warehouseId}              │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ POST /api/movements                       │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ POST /api/movements/import/{warehouseId}  │ Inventory    │ ❌ add to API                   │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ GET /api/transfers                        │ Inventory    │ ✅ in API — 🔴 500 until Step 0 │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ POST /api/transfers                       │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ PATCH /api/transfers/{id}/dispatch        │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ PATCH /api/transfers/{id}/accept          │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ PATCH /api/transfers/{id}/reject-delivery │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ POST /api/reconciliations                 │ Inventory    │ ✅ in API                       │
  ├───────────────────────────────────────────┼──────────────┼─────────────────────────────────┤
  │ GET /api/notifications                    │ Notification │ ✅ in API                       │
  └───────────────────────────────────────────┴──────────────┴─────────────────────────────────┘

  Files to change

  inventAlert-frontend/src/apis/inventAlertApi.js — add 1 endpoint:
  importMovementsCsv  POST /api/movements/import/{warehouseId}   (multipart/form-data, tag: Movement + Stock)

  Component audit: Verify every Warehouse Staff action (record movement, initiate transfer, dispatch/accept/reject-delivery, submit
  reconciliation) calls the RTK Query mutation hook, not a plain Redux dispatch to local state. Replace any remaining local-dispatch patterns.

  Acceptance criteria before Step 4

  - Login as staff1@pharmaplus.ng (assigned warehouse: Pharmaplus Ikeja Hub)
  - Stock view shows real stock levels for the assigned warehouse only
  - Record an INTAKE movement (product + quantity) → stock level in DB increases → UI re-renders without manual refresh
  - Record an OUTBOUND_SALE movement that drops stock below threshold → restock alert fires → visible to Procurement Officer in Step 4 test
  - Initiate a transfer to second warehouse → GET /api/transfers for Manager shows it as PENDING
  - After Manager approves (tested in Step 2): dispatch → accept delivery → status COMPLETED
  - Submit a reconciliation → visible to Manager as PENDING
  - Upload CSV file of intake movements → stock levels update in bulk
  - Staff logged in as staff2@pharmaplus.ng (different warehouse) cannot see staff1's warehouse stock

  ---
  Step 4 — Procurement Officer Dashboard

  Decision point — Purchase Orders

  The backend has an AlertController with acknowledge → order-placed → resolve lifecycle, but no PurchaseOrderController.
  purchaseOrdersSlice.js is Redux-only with no backend counterpart.

  Recommended resolution: Map the "Purchase Orders" UI to alerts in ORDER_PLACED status. An alert that has been acknowledged and marked
  order-placed is functionally a purchase order in progress. This avoids building a new backend entity with one week left before demo.

  - Keep purchaseOrdersSlice.js as a thin local UI slice (selected PO, modal state)
  - Populate the PO view with useGetAlertsQuery({ status: 'ORDER_PLACED' }) — these are the in-flight orders
  - The full alerts tab shows all statuses; the PO tab filters to ORDER_PLACED

  Endpoints consumed

  ┌────────────────────────────────────────┬──────────────┬────────────────────────────┐
  │                  Path                  │   Service    │           Status           │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ GET /api/alerts                        │ Inventory    │ ❌ add to API — Redux-only │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ PATCH /api/alerts/{id}/acknowledge     │ Inventory    │ ❌ add to API — Redux-only │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ PATCH /api/alerts/{id}/order-placed    │ Inventory    │ ❌ add to API — Redux-only │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ PATCH /api/alerts/{id}/resolve         │ Inventory    │ ❌ add to API — Redux-only │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ GET /api/analytics/alerts/summary      │ Analytics    │ ✅ in API                  │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ GET /api/analytics/alerts/by-warehouse │ Analytics    │ ✅ in API                  │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ GET /api/stock/{warehouseId}           │ Inventory    │ ✅ in API                  │
  ├────────────────────────────────────────┼──────────────┼────────────────────────────┤
  │ GET /api/notifications                 │ Notification │ ✅ in API                  │
  └────────────────────────────────────────┴──────────────┴────────────────────────────┘

  Files to change

  inventAlert-frontend/src/apis/inventAlertApi.js — add 4 endpoints:
  getAlerts           GET   /api/alerts?page=&size=&status=&warehouseId=   (tag: Alert)
  acknowledgeAlert    PATCH /api/alerts/{id}/acknowledge                    (invalidates: Alert)
  markAlertOrderPlaced PATCH /api/alerts/{id}/order-placed                 (invalidates: Alert)
  resolveAlert        PATCH /api/alerts/{id}/resolve                       (invalidates: Alert)

  inventAlert-frontend/src/store/slices/alertsSlice.js — replace all mock state and local reducers with RTK Query hooks in components; keep
  slice only for local UI (active alert id, drawer open).

  inventAlert-frontend/src/store/slices/purchaseOrdersSlice.js — remove mock PO data; keep for UI state only; Purchase Orders view uses
  useGetAlertsQuery({ status: 'ORDER_PLACED' }).

  Acceptance criteria before Step 5

  - Login as procurement@pharmaplus.ng
  - Alerts list shows real restock triggers generated by the Step 3 outbound sale
  - Acknowledge an alert → status ACKNOWLEDGED; list refreshes without page reload
  - Mark order placed → status ORDER_PLACED
  - Resolve alert → status RESOLVED; alert no longer counts in badge
  - "Purchase Orders" view shows only ORDER_PLACED alerts as in-progress orders
  - Alert analytics chart (summary + by-warehouse) renders real data
  - No mock arrays visible in Redux DevTools for alertsSlice or purchaseOrdersSlice

  ---
  Step 5 — SuperAdmin Dashboard

  Endpoints consumed

  ┌─────────────────────────────────────────────────┬───────────┬────────────────────────────┐
  │                      Path                       │  Service  │           Status           │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ GET /api/superadmin/companies                   │ Identity  │ ❌ add to API — Redux-only │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ PATCH /api/superadmin/companies/{id}/suspend    │ Identity  │ ❌ add to API — Redux-only │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ PATCH /api/superadmin/companies/{id}/reactivate │ Identity  │ ❌ add to API — Redux-only │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ GET /api/complaints                             │ Identity  │ ❌ add to API — Redux-only │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ POST /api/complaints                            │ Identity  │ ❌ add to API — Redux-only │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ PATCH /api/complaints/{id}/resolve              │ Identity  │ ❌ add to API — Redux-only │
  ├─────────────────────────────────────────────────┼───────────┼────────────────────────────┤
  │ GET /api/analytics/companies/summary            │ Analytics │ ❌ add to API — Redux-only │
  └─────────────────────────────────────────────────┴───────────┴────────────────────────────┘

  Files to change

  inventAlert-frontend/src/apis/inventAlertApi.js — add 7 endpoints:
  getSuperAdminCompanies  GET   /api/superadmin/companies                  (tag: Company)
  suspendCompany          PATCH /api/superadmin/companies/{id}/suspend      (invalidates: Company)
  reactivateCompanySA     PATCH /api/superadmin/companies/{id}/reactivate   (invalidates: Company)
  getComplaints           GET   /api/complaints?page=&size=&status=         (tag: Complaint — add to tagTypes)
  submitComplaint         POST  /api/complaints                             (invalidates: Complaint)
  resolveComplaint        PATCH /api/complaints/{id}/resolve                (invalidates: Complaint)
  getCompanyAnalytics     GET   /api/analytics/companies/summary?months=    (tag: Analytics)

  inventAlert-frontend/src/store/slices/superadminSlice.js — remove all mock company and complaint data; keep slice only for local UI state
  (selected company, modal open, filter values).

  Acceptance criteria before Step 6

  - Login via SuperAdmin credentials
  - Companies list shows all 4 seeded companies with real ACTIVE status
  - Suspend Pharmaplus → status becomes SUSPENDED in DB; refresh confirms; Pharmaplus admin login should now fail or be blocked
  - Reactivate → status restored to ACTIVE; Pharmaplus admin can log in again
  - Complaints list loads (may be empty if none submitted); submit a complaint as a regular admin user → appears here
  - Resolve complaint → status RESOLVED
  - Company analytics chart renders growth data from ClickHouse
  - No mock arrays in Redux DevTools for superadminSlice

  ---
  Step 6 — WebSocket / Real-time Notifications (Cross-cutting)

  After all dashboards pass their acceptance criteria, verify the real-time notification layer works end-to-end across roles.

  Scope: native WebSocket connection (not SockJS — project CORS rule). If the WebSocket URL is currently hardcoded or pointing at a dev stub,
  wire it to the real notification service.

  Files to check:
  - WebSocket setup file in frontend (likely src/services/ or src/utils/)
  - notificationService WebSocket endpoint path in application config

  Acceptance criteria:
  - Login as Warehouse Staff; record a movement → Manager receives in-app notification within 5 seconds, no page refresh
  - Outbound sale triggers alert → Procurement Officer sees badge increment in real-time
  - Mark any notification as read → badge decrements immediately
  - No SockJS dependency in the WebSocket connection code

  ---
  Step 7 — Kubernetes / EC2 Deployment Preparation

  Complete only after all dashboards pass Step 6.

  7.1 — Dockerfile audit (each of the 4 services):
  - Use multi-stage build: FROM maven:3.9-eclipse-temurin-21 AS build → FROM eclipse-temurin:21-jre-alpine
  - Remove COPY target/*.jar patterns that require a pre-built jar; build inside Docker
  - Confirm no credentials baked into Dockerfile ENV lines

  7.2 — Create k8s/ directory with manifests:

  Manifest: namespace.yaml
  Type: Namespace
  Notes: inventalert-prod
  ────────────────────────────────────────
  Manifest: secrets.yaml
  Type: Secret
  Notes: JWT_SECRET, DB passwords, SMTP credentials — never commit real values; use a template with <REPLACE_ME>
  ────────────────────────────────────────
  Manifest: configmap-nginx.yaml
  Type: ConfigMap
  Notes: contents of nginx.conf
  ────────────────────────────────────────
  Manifest: mysql-statefulset.yaml
  Type: StatefulSet + PVC
  Notes: 20Gi gp2 EBS, mysql:8 image
  ────────────────────────────────────────
  Manifest: redis-statefulset.yaml
  Type: StatefulSet + PVC
  Notes: 5Gi, redis:7-alpine
  ────────────────────────────────────────
  Manifest: kafka-statefulset.yaml
  Type: StatefulSet + PVC
  Notes: 10Gi, KRaft mode, same image as docker-compose
  ────────────────────────────────────────
  Manifest: clickhouse-statefulset.yaml
  Type: StatefulSet + PVC
  Notes: 20Gi
  ────────────────────────────────────────
  Manifest: identity-deployment.yaml
  Type: Deployment + Service
  Notes: 2 replicas, readinessProbe: /actuator/health
  ────────────────────────────────────────
  Manifest: inventory-deployment.yaml
  Type: Deployment + Service
  Notes: 2 replicas
  ────────────────────────────────────────
  Manifest: analytics-deployment.yaml
  Type: Deployment + Service
  Notes: 1 replica (ClickHouse bottleneck)
  ────────────────────────────────────────
  Manifest: notification-deployment.yaml
  Type: Deployment + Service
  Notes: 2 replicas
  ────────────────────────────────────────
  Manifest: nginx-deployment.yaml
  Type: Deployment + Service (LoadBalancer)
  Notes: 1 replica
  ────────────────────────────────────────
  Manifest: ingress.yaml
  Type: Ingress
  Notes: nginx Ingress Controller; route /api/auth, /api/users, /api/companies, /api/superadmin, /api/complaints → identity; /api/warehouses,
    /api/products, /api/stock, /api/movements, /api/transfers, /api/reconciliations, /api/alerts → inventory; /api/analytics → analytics;
    /api/notifications → notification

  7.3 — EC2 node sizing (minimum for demo):
  - 1× t3.large for stateful infra (MySQL, Redis, Kafka, ClickHouse)
  - 4× t3.small for app services (or use a single t3.xlarge node cluster for simplicity)

  7.4 — Pre-deployment checklist:
  - All 4 Spring Boot services expose GET /actuator/health and return {"status":"UP"}
  - application.properties / application.yml in each service reads DB host from ${DB_HOST} env var (not hardcoded mysql-identity Docker name)
  - Kafka bootstrap in each service reads from ${KAFKA_BOOTSTRAP_SERVERS} env var
  - Frontend VITE_API_BASE_URL is set to the EC2 load balancer URL before npm run build
  - docker compose --profile app up still passes all 5 dashboard acceptance criteria on a clean local run before pushing to EC2

  ---
  Summary table — RTK Query endpoints to add (all steps)

  ┌──────┬───────────────────────────────────────────────────────┬────────┬─────────────────┐
  │ Step │                       Endpoint                        │ Method │       Tag       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 1    │ /api/products/{id}/threshold                          │ PATCH  │ Product         │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 1    │ /api/stock-levels/{productId}/{warehouseId}/threshold │ PATCH  │ Stock           │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 1    │ /api/analytics/stock/top-products                     │ GET    │ Analytics       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 1    │ /api/analytics/stock/movements/by-warehouse           │ GET    │ Analytics       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 2    │ /api/stock                                            │ GET    │ Stock           │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 2    │ /api/analytics/notifications/summary                  │ GET    │ Analytics       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 2    │ /api/notifications/unread-count                       │ GET    │ Notification    │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 3    │ /api/movements/import/{warehouseId}                   │ POST   │ Movement, Stock │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 4    │ /api/alerts                                           │ GET    │ Alert           │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 4    │ /api/alerts/{id}/acknowledge                          │ PATCH  │ Alert           │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 4    │ /api/alerts/{id}/order-placed                         │ PATCH  │ Alert           │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 4    │ /api/alerts/{id}/resolve                              │ PATCH  │ Alert           │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/superadmin/companies                             │ GET    │ Company         │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/superadmin/companies/{id}/suspend                │ PATCH  │ Company         │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/superadmin/companies/{id}/reactivate             │ PATCH  │ Company         │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/complaints                                       │ GET    │ Complaint       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/complaints                                       │ POST   │ Complaint       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/complaints/{id}/resolve                          │ PATCH  │ Complaint       │
  ├──────┼───────────────────────────────────────────────────────┼────────┼─────────────────┤
  │ 5    │ /api/analytics/companies/summary                      │ GET    │ Analytics       │
  └──────┴───────────────────────────────────────────────────────┴────────┴─────────────────┘

  19 new RTK Query endpoints total, all going into inventAlert-frontend/src/apis/inventAlertApi.js. Every backend controller for these already
   exists — no new Spring Boot code needed except the Step 0 Kafka publish fix.