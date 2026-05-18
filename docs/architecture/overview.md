# Architecture Overview

## System Diagram

![High-Level Architecture](../../Design%20Diagrams/High%20Level%20Architecture.png)

---

## Service Map

InventAlert is a **microservices** system. Each service owns its data store and communicates either via synchronous REST (frontend ↔ Nginx ↔ services) or asynchronous events (Kafka).

```
Browser / Mobile
       │  HTTPS
       ▼
   Nginx :8080          ← API gateway — routes by URL prefix
   ┌────┬────┬────┬─────┐
   │    │    │    │     │
 :8081 :8082 :8083 :8084  ← Spring Boot services
Identity Inventory Notification Analytics
   │    │    │    │
   │    │    │    └── ClickHouse :8123  (OLAP)
   │    │    └─────── Redis :6379       (notification store)
   │    └──────────── MySQL :3308       (per-tenant schemas)
   └───────────────── MySQL :3308       (identity schema)
                            │
                         Kafka :9092    (event bus)
```

| Service | Port | Primary Store | Role |
|---|---|---|---|
| Identity | 8081 | MySQL `inventalert_identity` | Auth, users, companies, JWT |
| Inventory | 8082 | MySQL `tenant_{companyId}` | Stock, movements, alerts, transfers, reconciliations |
| Notification | 8083 | Redis | Real-time and REST notifications, email delivery |
| Analytics | 8084 | ClickHouse | Event ingestion and time-series OLAP queries |
| Nginx | 8080 | — | Reverse proxy, SSL termination, WebSocket upgrade |

---

## Kafka Event Flow

All domain events flow through Kafka. This decouples producers from consumers and allows new services to subscribe without modifying existing code.

```
Identity Service
  └─► company.created          → Inventory (create tenant schema), Analytics
  └─► company.offboarded       → Inventory (archive data), Analytics
  └─► password.reset.requested → Notification (send email)

Inventory Service
  └─► stock.movement.created   → Analytics
  └─► restock.alert.created    → Notification (in-app + email), Analytics
  └─► transfer.suggested       → Notification, Analytics
  └─► transfer.approved/rejected/dispatched/accepted/rejected-delivery
                               → Notification, Analytics
  └─► reconciliation.requested/approved/rejected
                               → Notification, Analytics

Notification Service
  └─► notification.events      → Analytics
```

---

## Multi-Tenancy Model

Each company gets an **isolated MySQL schema** (`tenant_{companyId}`) created automatically when the `company.created` Kafka event is consumed by the Inventory Service. This provides:

- **Data isolation** — no cross-company data leakage possible at the DB level
- **Independent schema evolution** — Flyway migrations per tenant
- **Easy offboarding** — drop the tenant schema to remove all company data

---

## Technology Stack

| Concern | Technology |
|---|---|
| Backend framework | Spring Boot 4.0.6 (Java 25) |
| Frontend | React 19.2, Vite 8, Redux Toolkit, Tailwind CSS 4 |
| Relational DB | MySQL 8 |
| Cache / Notifications | Redis 7 |
| Analytics OLAP | ClickHouse 24.3 |
| Message broker | Apache Kafka 7.6.0 |
| API gateway | Nginx 1.25 |
| Container orchestration | Kubernetes 1.32 on AWS EKS |
| Image registry | Amazon ECR |
| Frontend hosting | Amazon S3 |
| Monitoring | Prometheus + Grafana |
| Auth | JWT (JJWT 0.13.0), BCrypt |

---

## See Also

- [Scalability Strategy](scalability.md)
- [All Design Diagrams](../design/README.md)
- [Service Breakdown](../service-breakdown/README.md)
