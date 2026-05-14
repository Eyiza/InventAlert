#!/usr/bin/env bash
# InventAlert — full deployment script
# Run from the project root on a machine with: aws-cli, eksctl, kubectl, docker, node/npm
set -euo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────
AWS_REGION="${AWS_REGION:-us-east-1}"
CLUSTER_NAME="inventalert-cluster"
NAMESPACE="inventalert-prod"
S3_BUCKET="${S3_BUCKET:-}"   # set via env var, or we create one below

# ─── Helpers ──────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

check_prereqs() {
  info "Checking prerequisites..."
  for cmd in aws eksctl kubectl docker node npm; do
    command -v "$cmd" &>/dev/null || error "$cmd is not installed"
  done
  aws sts get-caller-identity &>/dev/null || error "AWS credentials not configured — run: aws configure"
  info "All prerequisites OK."
}

# ─── Step 0: Resolve AWS account ──────────────────────────────────────────────
check_prereqs
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
info "AWS account: $AWS_ACCOUNT_ID  region: $AWS_REGION  ECR: $ECR_URI"

# ─── Step 1: Verify secrets.yaml has no placeholders ─────────────────────────
info "Checking secrets.yaml..."
if grep -q '<REPLACE_ME>' k8s/secrets.yaml; then
  error "k8s/secrets.yaml still contains <REPLACE_ME> values. Fill them in before running this script."
fi
info "secrets.yaml looks complete."

# ─── Step 2: Create ECR repositories ─────────────────────────────────────────
info "Creating ECR repositories..."
for svc in identity-service inventory-service analytics-service notification-service; do
  aws ecr describe-repositories --repository-names "inventalert-${svc}" --region "$AWS_REGION" &>/dev/null \
    || aws ecr create-repository --repository-name "inventalert-${svc}" --region "$AWS_REGION" --output text --query 'repository.repositoryUri'
  info "  inventalert-${svc} ready"
done

# ─── Step 3: Build and push Docker images ────────────────────────────────────
info "Logging in to ECR..."
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_URI"

declare -A SERVICE_DIRS=(
  ["identity-service"]="identityService"
  ["inventory-service"]="inventoryService"
  ["analytics-service"]="analyticsService"
  ["notification-service"]="notificationService"
)

for svc in "${!SERVICE_DIRS[@]}"; do
  dir="${SERVICE_DIRS[$svc]}"
  image="${ECR_URI}/inventalert-${svc}:latest"
  info "Building $svc from ./$dir ..."
  docker build -t "$image" "./$dir"
  info "Pushing $image ..."
  docker push "$image"
done

# ─── Step 4: Create EKS cluster ──────────────────────────────────────────────
if eksctl get cluster --name "$CLUSTER_NAME" --region "$AWS_REGION" &>/dev/null; then
  info "Cluster $CLUSTER_NAME already exists — skipping creation."
else
  info "Creating EKS cluster (this takes ~15 minutes)..."
  eksctl create cluster -f eksctl-cluster.yaml
fi

# Update local kubeconfig
aws eks update-kubeconfig --name "$CLUSTER_NAME" --region "$AWS_REGION"
kubectl cluster-info

# ─── Step 5: Namespace ────────────────────────────────────────────────────────
info "Applying namespace..."
kubectl apply -f k8s/namespace.yaml

# ─── Step 6: ConfigMaps ───────────────────────────────────────────────────────
info "Creating init-SQL ConfigMaps..."
kubectl create configmap mysql-init-sql \
  --from-file=init-inventory.sql=./docker/init-inventory.sql \
  -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap clickhouse-init-sql \
  --from-file=init.sql=./docker/clickhouse-init.sql \
  -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f k8s/configmap-nginx.yaml

# ─── Step 7: Secrets ──────────────────────────────────────────────────────────
info "Applying secrets..."
kubectl apply -f k8s/secrets.yaml

# ─── Step 8: Stateful infrastructure ─────────────────────────────────────────
info "Deploying MySQL..."
kubectl apply -f k8s/mysql-statefulset.yaml
info "Deploying Redis..."
kubectl apply -f k8s/redis-statefulset.yaml
info "Deploying Kafka..."
kubectl apply -f k8s/kafka-statefulset.yaml
info "Deploying ClickHouse..."
kubectl apply -f k8s/clickhouse-statefulset.yaml

info "Waiting for stateful services to become ready (up to 10 min)..."
kubectl rollout status statefulset/mysql      -n "$NAMESPACE" --timeout=600s
kubectl rollout status statefulset/redis      -n "$NAMESPACE" --timeout=180s
kubectl rollout status statefulset/kafka      -n "$NAMESPACE" --timeout=300s
kubectl rollout status statefulset/clickhouse -n "$NAMESPACE" --timeout=600s
info "All stateful services are ready."

# ─── Step 9: Application deployments ─────────────────────────────────────────
info "Deploying application services..."
for manifest in \
  k8s/identity-deployment.yaml \
  k8s/inventory-deployment.yaml \
  k8s/analytics-deployment.yaml \
  k8s/notification-deployment.yaml \
  k8s/nginx-deployment.yaml; do
  sed "s|<ECR_URI>|${ECR_URI}|g" "$manifest" | kubectl apply -f -
done

info "Waiting for application deployments..."
for deploy in identity-service inventory-service analytics-service notification-service nginx; do
  kubectl rollout status deployment/"$deploy" -n "$NAMESPACE" --timeout=300s
done
info "All application services are ready."

# ─── Step 10: Get the Load Balancer hostname ──────────────────────────────────
info "Waiting for LoadBalancer IP/hostname (up to 3 min)..."
for i in $(seq 1 18); do
  LB_HOST=$(kubectl get service nginx -n "$NAMESPACE" \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)
  [ -n "$LB_HOST" ] && break
  sleep 10
done
[ -z "$LB_HOST" ] && error "LoadBalancer hostname not available after 3 minutes. Check: kubectl get svc nginx -n $NAMESPACE"
info "Load Balancer: $LB_HOST"

# ─── Step 11: Build and deploy frontend to S3 ────────────────────────────────
if [ -z "$S3_BUCKET" ]; then
  S3_BUCKET="inventalert-frontend-${AWS_ACCOUNT_ID}"
fi

info "Setting up S3 bucket: $S3_BUCKET"
aws s3api head-bucket --bucket "$S3_BUCKET" 2>/dev/null \
  || aws s3 mb "s3://${S3_BUCKET}" --region "$AWS_REGION"

# Enable static website hosting
aws s3 website "s3://${S3_BUCKET}" \
  --index-document index.html \
  --error-document index.html

# Allow public reads (required for static hosting)
aws s3api put-public-access-block \
  --bucket "$S3_BUCKET" \
  --public-access-block-configuration \
    "BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false"

aws s3api put-bucket-policy --bucket "$S3_BUCKET" --policy "{
  \"Version\": \"2012-10-17\",
  \"Statement\": [{
    \"Effect\": \"Allow\",
    \"Principal\": \"*\",
    \"Action\": \"s3:GetObject\",
    \"Resource\": \"arn:aws:s3:::${S3_BUCKET}/*\"
  }]
}"

info "Building frontend with VITE_API_BASE_URL=http://${LB_HOST} ..."
(
  cd inventAlert-frontend
  VITE_API_BASE_URL="http://${LB_HOST}" npm run build
)

info "Uploading frontend to S3..."
aws s3 sync inventAlert-frontend/dist/ "s3://${S3_BUCKET}" --delete

FRONTEND_URL="http://${S3_BUCKET}.s3-website-${AWS_REGION}.amazonaws.com"

# ─── Done ─────────────────────────────────────────────────────────────────────
echo ""
echo "================================================================"
echo "  Deployment complete"
echo "  Frontend : ${FRONTEND_URL}"
echo "  API      : http://${LB_HOST}"
echo ""
echo "  Set CORS_ALLOWED_ORIGINS in secrets.yaml to: ${FRONTEND_URL}"
echo "  Then re-run: kubectl apply -f k8s/secrets.yaml"
echo "  And restart services: kubectl rollout restart deployment -n ${NAMESPACE}"
echo "================================================================"
