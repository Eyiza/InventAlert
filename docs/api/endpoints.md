# All API Endpoints

## Identity Service (port 8081)

### Auth — `/api/auth`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/signup` | Public | Register company + first ADMIN user |
| POST | `/api/auth/login` | Public | Authenticate and receive JWT |
| POST | `/api/auth/superadmin/login` | Public | Super admin login |
| POST | `/api/auth/forgot-password` | Public | Send password reset email |
| POST | `/api/auth/reset-password` | Public | Reset password with token |
| GET | `/api/auth/me` | Any | Refresh token / get session |
| POST | `/api/auth/change-password` | Any | Change own password |

### Users — `/api/users`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/users` | ADMIN, MANAGER | Create a user |
| GET | `/api/users` | ADMIN, MANAGER | List company users |
| PATCH | `/api/users/{id}/role` | ADMIN | Update user role |
| PATCH | `/api/users/{id}/deactivate` | ADMIN | Deactivate user |
| PATCH | `/api/users/{id}/reactivate` | ADMIN | Reactivate user |
| POST | `/api/users/{id}/assign` | ADMIN | Assign user to warehouse |
| GET | `/api/users/{id}/assignments` | ADMIN | Get warehouse assignments |
| DELETE | `/api/users/{id}/assignments/{assignmentId}` | ADMIN | Remove assignment |

### Companies — `/api/companies`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/companies/me` | ADMIN | Get own company profile |
| PATCH | `/api/companies/me` | ADMIN | Update company profile |
| POST | `/api/companies/offboard` | ADMIN | Initiate company offboarding |

### Super Admin — `/api/superadmin`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/superadmin/companies` | SUPER_ADMIN | List all companies |
| PATCH | `/api/superadmin/companies/{id}/suspend` | SUPER_ADMIN | Suspend company |
| PATCH | `/api/superadmin/companies/{id}/reactivate` | SUPER_ADMIN | Reactivate company |
| GET | `/api/superadmin/complaints` | SUPER_ADMIN | List all complaints |
| PATCH | `/api/superadmin/complaints/{id}/resolve` | SUPER_ADMIN | Resolve complaint |

---

## Inventory Service (port 8082)

### Warehouses — `/api/warehouses`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/warehouses` | ADMIN | Create warehouse |
| GET | `/api/warehouses` | All | List active warehouses |
| PATCH | `/api/warehouses/{id}` | ADMIN | Update warehouse |
| PATCH | `/api/warehouses/{id}/deactivate` | ADMIN | Deactivate warehouse |
| PATCH | `/api/warehouses/{id}/activate` | ADMIN | Reactivate warehouse |

### Products — `/api/products`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/products` | ADMIN | Create product |
| GET | `/api/products` | All | List active products |
| PATCH | `/api/products/{id}` | ADMIN | Update product |
| PATCH | `/api/products/{id}/threshold` | ADMIN | Set default reorder threshold |
| POST | `/api/products/import` | ADMIN | Bulk import from CSV |

### Stock Levels — `/api/stock`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/stock` | ADMIN | All stock levels (paginated) |
| GET | `/api/stock/{warehouseId}` | All | Stock for a warehouse |
| PATCH | `/api/stock-levels/{productId}/{warehouseId}/threshold` | ADMIN | Per-warehouse threshold |

### Stock Movements — `/api/movements`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/movements` | WAREHOUSE_STAFF, PROCUREMENT_OFFICER | Record intake or outbound |
| GET | `/api/movements` | ADMIN, MANAGER | List movements (filterable) |
| POST | `/api/movements/import/{warehouseId}` | WAREHOUSE_STAFF | Bulk CSV intake import |

### Restock Alerts — `/api/alerts`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/alerts` | PROCUREMENT_OFFICER, MANAGER | List alerts (filterable by status) |
| PATCH | `/api/alerts/{id}/acknowledge` | PROCUREMENT_OFFICER | Acknowledge alert |
| PATCH | `/api/alerts/{id}/order-placed` | PROCUREMENT_OFFICER | Mark order placed |
| PATCH | `/api/alerts/{id}/resolve` | PROCUREMENT_OFFICER | Resolve alert |

### Transfers — `/api/transfers`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/transfers` | WAREHOUSE_STAFF | Initiate transfer suggestion |
| GET | `/api/transfers` | MANAGER, WAREHOUSE_STAFF | List transfers (paginated) |
| PATCH | `/api/transfers/{id}/approve` | MANAGER | Approve suggestion |
| PATCH | `/api/transfers/{id}/reject` | MANAGER | Reject (escalates to alert) |
| PATCH | `/api/transfers/{id}/dispatch` | WAREHOUSE_STAFF | Dispatch from source |
| PATCH | `/api/transfers/{id}/accept` | WAREHOUSE_STAFF | Accept at destination |
| PATCH | `/api/transfers/{id}/reject-delivery` | WAREHOUSE_STAFF | Reject delivery (restores stock) |

### Reconciliations — `/api/reconciliations`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/reconciliations` | WAREHOUSE_STAFF | Submit physical count |
| GET | `/api/reconciliations` | ADMIN, MANAGER | List reconciliations (paginated) |
| PATCH | `/api/reconciliations/{id}/approve` | MANAGER | Approve (adjusts stock) |
| PATCH | `/api/reconciliations/{id}/reject` | MANAGER | Reject (no stock change) |

---

## Notification Service (port 8083)

### Notifications — `/api/notifications`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/notifications` | Any | Get paginated notifications |
| PATCH | `/api/notifications/{id}/read` | Any | Mark as read |
| GET | `/api/notifications/unread-count` | Any | Get unread badge count |

---

## Analytics Service (port 8084)

### Stock Analytics — `/api/analytics/stock`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/analytics/stock/summary` | ADMIN, MANAGER | Intake/outbound summary |
| GET | `/api/analytics/stock/top-products` | ADMIN, MANAGER | Top moving products |
| GET | `/api/analytics/stock/movements/trend` | ADMIN, MANAGER | Daily movement trend |
| GET | `/api/analytics/stock/movements/by-warehouse` | ADMIN, MANAGER | Per-warehouse breakdown |

### Alert Analytics — `/api/analytics/alerts`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/analytics/alerts/summary` | ADMIN, MANAGER, PROCUREMENT_OFFICER | Alert summary |
| GET | `/api/analytics/alerts/by-warehouse` | ADMIN, MANAGER, PROCUREMENT_OFFICER | Per-warehouse alert count |

### Transfer Analytics — `/api/analytics/transfers`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/analytics/transfers/summary` | ADMIN, MANAGER | Transfer efficiency summary |

### Company Analytics — `/api/analytics/companies`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/analytics/companies/summary` | SUPER_ADMIN | Platform onboarding metrics |

### Notification Analytics — `/api/analytics/notifications`

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/analytics/notifications/summary` | ADMIN, MANAGER | Notification delivery stats |
