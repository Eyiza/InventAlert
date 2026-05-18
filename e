[33mcommit 6a55469d52ef6b76961dd51f3d586d282109be75[m
Author: Chukwuebuka Samuel Nwafor <145198300+NwaforChukwuebuka@users.noreply.github.com>
Date:   Thu May 14 19:07:46 2026 +0100

    ci: production-grade CI/CD pipeline + k8s hardening
    
    GitHub Actions:
    - Two-job pipeline: `ci` (build on every push/PR) and `deploy` (production only)
    - Immutable SHA-tagged Docker images; :latest also pushed for convenience
    - kubectl set image pins exact tag after apply so rollbacks are deterministic
    - Auto-rollback via kubectl rollout undo if deploy job times out or crashes
    - concurrency group cancels queued runs on fast pushes
    - Switched Docker image loop from declare -A to colon-delimited pairs (portable)
    
    Kubernetes hardening:
    - RollingUpdate strategy (maxSurge=0, maxUnavailable=1) on all app deployments
    - identity/inventory/nginx replicas reduced to 1 to fit 2×t3.medium node budget
    - Kafka: fsGroup=1000, dedicated log subdir, heap cap (256m/512m), TCP probes
    - inventory-service: reduced resource requests; APP_SEED_DATA=false guard
    - analytics-service: CORS_ALLOWED_ORIGINS injected from secret
    - nginx: TCP readiness probe + tcp backend-protocol annotation on ELB
    
    Security:
    - Remove accidentally-staged JWT_SECRET value from .env.example
    
    eksctl-cluster.yaml: upgrade k8s version 1.29→1.32; switch to 2×t3.medium
    
    [1;31mCo-Authored-By: Claude[m Sonnet 4.6 <noreply@anthropic.com>

[33mcommit bcf3c576aeaa167b1aeac2f82f7e92e32e2cebe2[m
Merge: 38fb8cd ca94574
Author: Chukwuebuka Samuel Nwafor <145198300+NwaforChukwuebuka@users.noreply.github.com>
Date:   Tue May 12 15:27:10 2026 +0100

    Merge branch 'analyticservice' into main
    
    [1;31mCo-Authored-By: Claude[m Sonnet 4.6 <noreply@anthropic.com>
