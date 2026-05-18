# Deployment Guide

## Overview

InventAlert is deployed to **AWS EKS** (Kubernetes 1.32) in **eu-west-1** (Ireland). The CI/CD pipeline is GitHub Actions.

```
GitHub Push → Actions (CI) → Build JARs → Docker Build → Push to ECR → kubectl apply → EKS
                                                                            └─► S3 (frontend)
```

---

## CI/CD Pipeline

**File:** `.github/workflows/deploy.yml`

| Job | Trigger | Steps |
|---|---|---|
| `ci` (Build & Validate) | Every push + PR to `main`/`production` | Run unit tests, package all 4 services |
| `deploy` (Deploy to EKS) | Push to `production` branch only | Build JARs, push Docker images to ECR, apply K8s manifests, wait for rollout, rollback on failure, sync frontend to S3 |

### Image Tagging Strategy

Images are tagged with the **8-character commit SHA** (e.g., `abc12345`). The `:latest` tag is also pushed but Kubernetes manifests are pinned to the SHA tag — this ensures rollbacks are deterministic.

```bash
# Manual rollback example
kubectl rollout undo deployment/inventory-service -n inventalert-prod
```

---

## Kubernetes Cluster

**File:** `eksctl-cluster.yaml`

| Setting | Value |
|---|---|
| Region | eu-west-1 (Ireland) |
| Kubernetes version | 1.32 |
| Node type | t3.medium (2 vCPU / 4 GB) |
| Node count | 2 |
| EBS CSI driver | Enabled (for persistent volumes) |

### Manifests (`k8s/`)

| File | Resource |
|---|---|
| `namespace.yaml` | `inventalert-prod` namespace |
| `secrets.yaml` | JWT secret, DB passwords, SMTP credentials |
| `configmap-nginx.yaml` | Nginx routing configuration |
| `identity-deployment.yaml` | Identity Service Deployment + Service |
| `inventory-deployment.yaml` | Inventory Service Deployment + Service |
| `notification-deployment.yaml` | Notification Service Deployment + Service |
| `analytics-deployment.yaml` | Analytics Service Deployment + Service |
| `nginx-deployment.yaml` | Nginx API gateway Deployment + Service |
| `mysql-statefulset.yaml` | MySQL StatefulSet + PersistentVolumeClaim |
| `redis-statefulset.yaml` | Redis StatefulSet + PersistentVolumeClaim |
| `kafka-statefulset.yaml` | Kafka StatefulSet |
| `clickhouse-statefulset.yaml` | ClickHouse StatefulSet |
| `ingress.yaml` | Kubernetes Ingress (external access) |
| `monitoring.yaml` | Prometheus + Grafana |

### Resource Limits

Each backend service is configured with:

```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 500m
    memory: 768Mi
```

### Health Checks

- **Readiness probe:** `GET /actuator/health` — delays 45s, checks every 10s, 6 retries
- **Liveness probe:** `GET /actuator/health` — delays 90s, checks every 20s

The notification service has the mail health check disabled (`management.health.mail.enabled=false`) to prevent liveness probe timeouts.

---

## Required GitHub Secrets

| Secret | Purpose |
|---|---|
| `AWS_ACCESS_KEY_ID` | ECR login and EKS kubeconfig |
| `AWS_SECRET_ACCESS_KEY` | ECR login and EKS kubeconfig |
| `S3_BUCKET` | Frontend static asset bucket name |
| `VITE_API_BASE_URL` | Frontend API base URL for the build |
| `VITE_GOOGLE_MAPS_API_KEY` | Google Maps API key for distance display |

---

## Manual Deployment

```bash
# 1. Create the cluster (first time only)
eksctl create cluster -f eksctl-cluster.yaml

# 2. Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/

# 3. Check rollout status
kubectl rollout status deployment/identity-service -n inventalert-prod
```
