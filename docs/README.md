# InventAlert — Documentation Index

InventAlert is a multi-tenant, microservices-based inventory management platform built for warehousing businesses across multiple locations. This folder contains all project documentation organised by concern.

---

## Quick Navigation

| Section | What's inside |
|---|---|
| [Architecture](architecture/overview.md) | High-level design, service map, Kafka event flow, scalability strategy |
| [API Reference](api/README.md) | Swagger UI links, auth guide, all endpoints table, WebSocket guide |
| [Guides](guides/local-setup.md) | Local dev setup, Docker Compose, EKS deployment |
| [Design](design/README.md) | ERD, class diagram, activity & sequence diagrams, PRD |
| [Service Breakdown](service-breakdown/README.md) | Per-service responsibilities, tech choices, and data ownership |

---

## Service Swagger UIs

When running locally each service exposes an interactive Swagger UI:

| Service | Port | Swagger UI |
|---|---|---|
| Identity Service | 8081 | http://localhost:8081/swagger-ui.html |
| Inventory Service | 8082 | http://localhost:8082/swagger-ui.html |
| Notification Service | 8083 | http://localhost:8083/swagger-ui.html |
| Analytics Service | 8084 | http://localhost:8084/swagger-ui.html |

> Via the Nginx gateway (port 8080): prefix each path with `/identity`, `/inventory`, `/notification`, or `/analytics`.

---

## Repository Layout

```
InventAlert/
├── identityService/          Spring Boot — auth, users, companies, roles
├── inventoryService/         Spring Boot — warehouses, products, stock, alerts, transfers
├── notificationService/      Spring Boot — real-time notifications (Redis + WebSocket)
├── analyticsService/         Spring Boot — OLAP analytics on ClickHouse
├── inventAlert-frontend/     React 19 + Redux Toolkit + Vite
├── k8s/                      Kubernetes manifests (EKS eu-west-1)
├── monitoring/               Prometheus + Grafana configuration
├── docker/                   DB initialisation scripts
├── Design Diagrams/          Architecture, ERD, UML, activity, and use case diagrams
└── docs/                     ← you are here
```

---

## Key Documentation Files at Root

| File | Purpose |
|---|---|
| [README.md](../README.md) | Quick-start and port reference |
| [CODEBASE_EXPLANATION.md](../CODEBASE_EXPLANATION.md) | Beginner-friendly architecture overview |
| [SENIOR_DEV_HANDOFF.md](../SENIOR_DEV_HANDOFF.md) | Detailed technical handoff (43 KB) |
| [erd.md](../erd.md) | Mermaid entity-relationship diagram |
| [prd.md](../prd.md) | Full product requirements document (54 KB) |
| [SEED_USERS.md](../SEED_USERS.md) | Test user credentials for all roles |
