# Follow-up Tasks

## Warehouse Microservice (`warehouseService`)

The identity service accepts `warehouseId` on user creation and warehouse assignment but does not own or create warehouses. A dedicated `warehouseService` must be built.

### Stack
Same as identity: Spring Boot, Spring Data JPA, MySQL, Kafka, JWT validation (no auth issuing).

### Entity

```java
Warehouse {
    String id;          // UUID, generated
    String companyId;   // scoped from JWT
    String name;
    String location;
    CompanyStatus status; // ACTIVE | SUSPENDED
    LocalDateTime createdAt;
}
```

### REST Endpoints

| Method   | Endpoint                  | Role            | Description               |
|----------|---------------------------|-----------------|---------------------------|
| `POST`   | `/api/warehouses`         | `ADMIN`         | Create a warehouse        |
| `GET`    | `/api/warehouses`         | `ADMIN`,`MANAGER` | List company warehouses |
| `GET`    | `/api/warehouses/{id}`    | `ADMIN`,`MANAGER` | Get a warehouse by ID   |
| `DELETE` | `/api/warehouses/{id}`    | `ADMIN`         | Delete a warehouse        |

All endpoints scope by `companyId` extracted from the JWT claim — same token issued by the identity service.

### Kafka Consumer

Consume `company.offboarded` (published by identity service on `DELETE /api/companies/me`) and delete all warehouses belonging to that `companyId`.

```java
@KafkaListener(topics = "company.offboarded")
public void onCompanyOffboarded(Map<String, Object> event) {
    String companyId = (String) event.get("companyId");
    warehouseRepository.deleteAllByCompanyId(companyId);
}
```

### Integration with Identity Service

No changes needed in the identity service. The expected flow:

1. Company signs up → identity service creates company, publishes `company.created`
2. Company calls `POST /api/warehouses` → warehouse service creates warehouse, returns `warehouseId`
3. Company calls `POST /api/users` with that `warehouseId` → identity service stores `WarehouseAssignment`
4. Company offboards → identity service deletes users/assignments/company, publishes `company.offboarded` → warehouse service deletes warehouses
