# InventAlert Codebase Explanation

This document explains the InventAlert project from a beginner-friendly Spring Boot perspective. The biggest thing to understand first is that InventAlert is not one Spring Boot app. It is a small multi-service system for multi-company, multi-warehouse inventory management.

Compared with a simpler single-backend Spring Boot project, InventAlert is split into separate services:

- `identityService`: login, signup, users, companies, roles, and password reset.
- `inventoryService`: warehouses, products, stock, movements, alerts, transfers, and reconciliations.
- `notificationService`: Redis-backed notifications, email, and WebSocket live updates.
- `analyticsService`: ClickHouse analytics built from Kafka events.
- `inventAlert-frontend`: React/Vite frontend using Redux Toolkit Query.
- Docker infrastructure: MySQL, Redis, Kafka, ClickHouse, and Nginx.

The core business idea is simple: companies manage stock across warehouses. Staff record stock movement. Low stock triggers either a transfer suggestion or a restock alert. Managers approve sensitive actions. Procurement handles restocking. The system keeps history so stock changes are auditable.

## Architecture

The usual request flow is:

```text
React frontend
  -> Nginx API gateway
  -> one Spring Boot service
  -> database / Redis / Kafka
  -> other services react to Kafka events
```

Nginx routes requests by URL prefix:

- `/api/auth`, `/api/users`, `/api/companies` go to `identityService`.
- `/api/products`, `/api/stock`, `/api/movements`, `/api/transfers`, `/api/reconciliations`, `/api/alerts` go to `inventoryService`.
- `/api/notifications` goes to `notificationService`.
- `/api/analytics` goes to `analyticsService`.
- `/ws` goes to `notificationService` for WebSocket updates.

This means the frontend can call one base URL while Nginx decides which backend should handle each request.

## Why The Project Uses Microservices

Each service owns a distinct responsibility:

- Identity owns who the user is.
- Inventory owns stock and stock rules.
- Notification owns user messages.
- Analytics owns reports and trends.

This keeps important workflows independent. For example, recording a stock movement should not wait for email or analytics to finish. The inventory service records the movement, publishes a Kafka event, and other services react later.

## Common Spring Boot Pattern

Most backend services follow this shape:

```text
controller -> service -> repository -> model/database
```

- Controllers receive HTTP requests.
- Services contain business rules.
- Repositories talk to the database.
- Models represent database entities.
- DTOs shape request and response bodies.
- Exceptions turn business errors into clean API responses.
- Kafka producers and consumers connect services asynchronously.

This is the same basic Spring Boot pattern as a beginner CRUD app, just repeated across several services.

## Identity Service

`identityService` handles:

- Company signup
- Login
- Super admin login
- Password reset
- User creation
- Role changes
- Warehouse assignments
- Company suspension/reactivation
- Complaints

During signup, the service does three important things:

1. Creates a `Company`.
2. Creates the first `ADMIN` user for that company.
3. Publishes a `company.created` Kafka event.

That event matters because the inventory service listens for it and creates that company's inventory database schema.

The main user roles are:

```text
ADMIN
MANAGER
WAREHOUSE_STAFF
PROCUREMENT_OFFICER
```

Login returns a JWT token containing information such as user id, company id, role, and sometimes warehouse id. Other services use that token to know who is making the request.

Passwords are hashed with BCrypt. The system does not store plain passwords.

## Inventory Service

`inventoryService` contains most of the business logic. It owns:

- Warehouses
- Products
- Stock levels
- Stock movements
- Restock alerts
- Transfer suggestions
- Reconciliations

A stock intake flow looks like this:

```text
validate movement type
validate staff belongs to warehouse
get or create stock level
save movement history
increase current stock
publish Kafka event
```

An outbound sale flow looks like this:

```text
validate movement type
validate staff belongs to warehouse
reject if stock would go negative
save movement history
decrease current stock
publish Kafka event
recalculate velocity
check threshold
```

The negative-stock check is one of the most important business rules. It prevents the system from saying a warehouse has less than zero of a product.

## Thresholds And Alerts

Threshold checking happens after stock changes. The service asks:

1. Is current stock still above or equal to the threshold? If yes, do nothing.
2. Is there already an open alert for this product and warehouse? If yes, do nothing.
3. Does another warehouse have enough surplus stock?
4. If yes, create a transfer suggestion.
5. If no, create a restock alert.

This design is useful because the system tries to solve low stock internally before asking procurement to buy more.

## Transfers

Transfers move stock from one warehouse to another.

A transfer usually moves through these states:

```text
SUGGESTED -> APPROVED -> IN_TRANSIT -> COMPLETED
```

It can also become:

```text
REJECTED
DELIVERY_REJECTED
```

The approval flow exists so stock does not move casually. A manager approves the transfer, source warehouse staff dispatch it, and destination warehouse staff accept it.

When dispatch happens, stock leaves the source warehouse. When accept happens, stock enters the destination warehouse. This mirrors the real-world movement of goods.

## Reconciliation

Reconciliation is for when physical stock does not match system stock.

The flow is:

```text
staff submits discrepancy
manager approves or rejects
stock changes only after approval
creator cannot self-approve
```

This avoids silent stock editing. It creates an audit trail showing who reported the mismatch, what the system count was, what the physical count was, and who approved or rejected the correction.

## Multi-Company Design

This is one of the more advanced parts of InventAlert.

Identity stores all companies and users in shared identity tables. Inventory creates a separate database schema for each company:

```text
company_<companyId>
```

So if Company A and Company B both call `/api/products`, they are using the same endpoint, but they are not looking at the same database tables.

The inventory service reads `companyId` from the JWT token and routes the request to the right schema.

In plain English:

```text
Company A token -> company_A inventory tables
Company B token -> company_B inventory tables
```

That is how the system keeps tenant data separated.

## Notification Service

`notificationService` listens for Kafka events and creates user notifications.

Its flow is:

```text
receive event
deduplicate by eventId
save notification in Redis
add notification id to user's sorted set
increment unread count
send optional email
broadcast over WebSocket
```

Redis is used because notification feeds and unread counts need to be fast. WebSockets are used so the frontend can receive live updates without constantly refreshing.

## Analytics Service

`analyticsService` listens to Kafka events and stores them in ClickHouse.

ClickHouse is used for analytical queries such as:

- Count stock movements.
- Summarize alerts.
- Group transfers by status.
- Show monthly company growth.
- Show stock movement trends.

This keeps reporting separate from the transactional inventory database. The inventory database handles day-to-day stock operations; ClickHouse handles reporting.

## Frontend

The frontend is React with Redux Toolkit Query.

The central API client is `inventAlert-frontend/src/apis/inventAlertApi.js`. It defines hooks such as:

```js
useLoginMutation()
useGetWarehousesQuery()
useRecordMovementMutation()
useGetTransfersQuery()
useGetStockSummaryQuery()
```

Components use these hooks instead of manually calling `fetch`.

Routes are role-based:

- `ADMIN` goes to the admin dashboard.
- `MANAGER` goes to the manager dashboard.
- `WAREHOUSE_STAFF` goes to the staff dashboard.
- `PROCUREMENT_OFFICER` goes to the procurement dashboard.
- `SUPER_ADMIN` goes to the super admin portal.

The frontend stores auth data in Redux and local storage so the user stays logged in after refresh.

## Why Kafka Is Used

Kafka is used for event-driven communication.

Examples:

```text
Company signs up -> inventory creates schema
Stock movement recorded -> analytics stores event
Restock alert created -> notification service alerts user
Password reset requested -> notification service sends email
```

The original service publishes an event. Other services consume that event and do their own work.

This helps avoid tight coupling. Inventory does not need to directly call analytics or notification every time something happens.

## Why Flyway Is Used

Flyway manages database migrations.

Instead of manually changing the database, the project stores SQL files like:

```text
V1__create_companies.sql
V2__create_users.sql
V3__create_stock_levels.sql
```

Spring Boot runs these migrations when the service starts. This makes database structure versioned and repeatable.

## Security

All backend services use Spring Security with JWT authentication.

The usual setup is:

- Public endpoints such as login and signup are allowed.
- Protected endpoints require a valid JWT.
- CSRF is disabled because these are stateless APIs.
- Sessions are not used.
- CORS is configured so the frontend can call the backend.
- Method-level security can restrict actions by role.

The shared `JWT_SECRET` is important. All services must use the same secret so they can validate the same token.

## Docker Infrastructure

Docker Compose starts the infrastructure needed by the services:

- MySQL for identity and inventory data.
- Redis for notifications.
- Kafka for event communication.
- ClickHouse for analytics.
- Nginx as the API gateway.

The app can run in two ways:

- Full Docker stack for end-to-end testing.
- Docker infrastructure plus local Spring Boot services for development.

## Simplest Mental Model

Think of InventAlert like this:

```text
Identity says who you are.
Inventory decides what you are allowed to do with stock.
Kafka announces important things that happened.
Notification tells people.
Analytics remembers events for reports.
Frontend gives each role the right screen.
Docker runs the world around it.
```

The project is more advanced than a beginner Spring Boot CRUD app because it includes microservices, JWT across services, multi-tenancy, Kafka, Redis, ClickHouse, Flyway, Docker Compose, WebSockets, role-based routing, optimistic locking, and async workflows.

But underneath all that, the main backend pattern is still familiar:

```text
controller -> service -> repository -> database
```

