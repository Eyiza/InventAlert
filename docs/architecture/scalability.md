# Scalability Strategy

## Current Deployment Baseline

The production EKS cluster runs on `t3.medium` nodes (2 vCPU / 4 GB each). Each service is constrained to:

- CPU: 250m request / 500m limit
- Memory: 512Mi request / 768Mi limit

This fits comfortably within a 2-node cluster and can serve a medium-sized SaaS customer base.

---

## Horizontal Scaling

All four backend services are **stateless** — authentication context comes from the JWT, and no in-process state is held. This means any service can be scaled horizontally by increasing the replica count:

```yaml
# Example: scale inventory service to 3 replicas
kubectl scale deployment inventory-service --replicas=3 -n inventalert-prod
```

The Kubernetes `RollingUpdate` strategy (`maxSurge=0, maxUnavailable=1`) ensures zero-downtime deployments during scale-out.

**Kafka** acts as the backpressure valve — if the Notification or Analytics service is slower than producers, messages queue in Kafka rather than applying back-pressure to the Inventory Service.

---

## Database Scaling

### MySQL (Relational)
- **Per-tenant schema isolation** means tenant A's slow queries cannot affect tenant B.
- Vertical scaling (larger instance type) is the first lever for MySQL.
- Read replicas can be added and configured via `spring.datasource.replica.url` if read-heavy workloads emerge.

### Redis (Notifications)
- Redis is used for notification storage only (TTL-bound, not primary OLTP). A Redis Cluster can be substituted with no application code change — just update `spring.data.redis.cluster.nodes`.

### ClickHouse (Analytics)
- ClickHouse tables use `MergeTree` with **monthly partitioning** (`toYYYYMM(event_time)`). Old partitions can be detached and archived with `ALTER TABLE ... DETACH PARTITION`.
- ClickHouse natively scales horizontally via sharding and replication — a distributed table can be added in front of the current single-node setup when needed.

---

## Kafka Scalability

Topics are created with a single partition today. When throughput grows:

1. **Increase partition count** per topic to allow multiple consumers in the same group.
2. **Add consumer group instances** — Spring Kafka will rebalance automatically.
3. **Separate consumer groups** for analytics vs. notifications mean a slow analytics consumer does not delay notification delivery.

---

## Async Processing

All threshold checks, velocity recalculations, and Kafka event publishing in the Inventory Service are executed on a dedicated `@Async` thread pool (`AsyncConfig`). This decouples response time from background work — a stock movement HTTP request returns 201 in < 10ms regardless of how long the threshold check takes.

---

## Retry and Resilience

- **RetryConfig** (Spring Retry) wraps Kafka consumers with exponential backoff.
- **Health checks** (readiness and liveness probes) prevent Kubernetes from routing traffic to a service that has not finished connecting to its database.
- **Init containers** in K8s manifests block service startup until MySQL and Kafka are ready, preventing connection-refused cascades on cold start.

---

## Future Scaling Paths

| Concern | Current | Next step |
|---|---|---|
| API throughput | 1 replica each | HPA on CPU/request-rate metric |
| MySQL | Single instance | Read replica or Aurora |
| Kafka | 1 partition per topic | Increase partitions + consumer instances |
| ClickHouse | Single node | Distributed cluster with replication |
| Frontend | S3 static | CloudFront CDN edge caching |
| Auth | Stateless JWT | No change needed |
