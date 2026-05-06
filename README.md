***InventAlert***  
A multi-warehouse company manages stock levels across locations. Warehouse staff can record stock intake and dispatch events for products. The system tracks current stock levels per product per warehouse. When stock for any product falls below a configurable reorder threshold, the system automatically raises a restock alert and notifies the relevant procurement officer. Managers can view current stock levels, movement history, and pending alerts. Admins configure products, warehouses, thresholds, and user assignments. The system must handle stock adjustments and reconciliations without creating negative stock levels.  
Actors: WarehouseStaff, ProcurementOfficer, Manager, Admin  
Core Entities: Product, Warehouse, StockLevel, StockMovement, RestockAlert, User  
Natural Kafka Trigger: Stock falls below threshold RestockAlertCreated event notifies →→procurement officer

***Functional Requirements***

1. A company shall be able to self-register their inventory system which creates a default admin user.  
2. Registered users shall be able to log in with email and password  
3. An Admin shall be able to create user accounts within their tenant and assign roles: Admin, Manager, WarehouseStaff, ProcurementOfficer.  
4. An Admin shall be able to assign WarehouseStaff users to one or more specific warehouses.  
5. An Admin should be able to manage warehouses, manage products and update user roles.  
6. WarehouseStaff shall be able to record stock intake (goods received) specifying product, quantity, and reference number. Stock level shall increase immediately.  
7. WarehouseStaff shall be able to record outbound sales (goods leaving the company) specifying product and quantity. The system shall reject the movement if it would result in negative stock.  
8. When stock falls below the reorder threshold, the system should send an alert.  
9. WarehouseStaff shall be able to submit a reconciliation request when physical count differs from system count, specifying the discrepancy and a reason.  
10. A Manager shall be able to approve or reject reconciliation requests. Stock levels shall only adjust upon approval. All reconciliations shall retain a full audit trail.

***Non functional Requirements***

1. Security on all actions.  
2. Reconciliation adjustments shall require manager approval before affecting stock. No staff member can self-approve.  
3. Performance \- less than 200ms latency  
   

***Actors***

- Admin  
- Manager  
- Warehouse Staff  
- Procurement Officer  
- Invent Alert Application

***Entities***

- Tenant  
- User  
- WarehouseAssignment  
- Warehouse  
- Product  
- StockLevel  
- StockMovement  
- RestockAlert  
- Reconciliation  
- Notification

***Design Diagrams:***

- ERD  
- Use case  
- Activity   
- UML  
- Sequence

***Additional:***

- Batch upload products  
- Platform customer care can be alerted of issues with the platform like the DeskFlow 

Future Core Entities:

- TransferSuggestion  
- DailyMovementSummary  
- AlertFrequency  
- TransferEfficiency