# V2 Comprehensive Plans

> V1 solves the problem. V2 transforms the business.

---

## V2 Philosophy

V1 proved the system works. V2 makes it smarter, safer, more inclusive, and production-hardened. Every item below maps directly to a feedback item, a reviewer question, or an unresolved edge case from the v1 presentation.

---

## Priority 1 — Bug Fixes (Must Ship Before V2 Demo)

These are v1 correctness issues that will undermine trust if still present during the next presentation.

### 1.1 Fix Transfer Suggestion Approval Flow

**Current (wrong):** Source warehouse sees the transfer suggestion → triggers alert.

**Correct flow:**
1. System detects source warehouse stock is below threshold
2. System identifies a surplus at a nearby destination warehouse
3. A transfer suggestion is created with status `PENDING_DESTINATION_APPROVAL`
4. **Destination warehouse manager** receives the request and decides to approve or reject
5. Only after destination manager approves does the suggestion become visible to the source warehouse manager as an actionable alert
6. Procurement Officer is notified of the approved suggestion — they can now decide whether to proceed with the internal transfer or place an external purchase order instead

**Data model change needed:**
- Add `PENDING_DESTINATION_APPROVAL` status to `TransferStatus` enum (before `SUGGESTED`)
- Add `destinationManagerApprovedAt` and `destinationManagerId` fields to `transfer_suggestions`
- New Kafka event: `transfer.destination.approved`

---

### 1.2 Warehouse-Scoped Procurement Officers

**Current (wrong):** A Procurement Officer is assigned to the entire company.

**Correct behavior:** A Procurement Officer is assigned to one or more specific warehouses (same as Managers and Staff).

**Change required:**
- The `warehouse_assignments` table already exists — Procurement Officers just need to be included in warehouse assignment logic
- Role assignment UI must require warehouse selection when creating a Procurement Officer
- All procurement-officer-specific queries must filter by `warehouseId`, not just `companyId`
- Restock alerts must be routed only to the Procurement Officer(s) assigned to that specific warehouse

---

### 1.3 Fix Manager Role Update UI

**Issue:** The update form/flow for manager-level users has a visual or functional bug.

**Action:** Identify the broken component in `ManagerDashboard.jsx` or the relevant update endpoint and repair it.

---

### 1.4 Cursor Pagination on Product Movements

**Issue:** Movement history has no pagination — all records are returned at once.

**Change required:**
- Add cursor-based pagination to `GET /api/movements` (20 records per page)
- Use `movementId` or `createdAt` as the cursor
- Frontend movement table must support next/previous page navigation
- Prevents memory issues on high-volume warehouses

---

### 1.5 Alert Filtering by Warehouse

**Issue:** Alert views do not filter by warehouse, making them unusable for multi-warehouse companies.

**Change required:**
- Add `warehouseId` query parameter to `GET /api/alerts`
- Frontend alert views must include a warehouse selector/filter
- Dashboard summary counts should be warehouse-aware

---

### 1.6 Purchase Order Audit Trail

**Issue:** No dedicated audit log for purchase order decisions.

**Change required:**
- Create `purchase_order_events` table (or ClickHouse event type) recording every PO-related action: created, approved, rejected, received, cancelled
- Expose this as a timeline on the Procurement Officer dashboard
- Each event must record: actor (userId), timestamp, notes/reason

---

## Priority 2 — V2 Core Features

### 2.1 Batch / Lot Tracking with Expiry Dates

**Why:** The panel specifically asked about loss due to expiration. Currently invisible.

**What to build:**
- Add `batchNumber`, `expiryDate`, and `receivedDate` to intake movements
- Stock is consumed FIFO (first-expiry-first-out) for outbound movements
- Expiry alerts fire X days before expiry (configurable per product, e.g., 30 days)
- Dashboard widget: "Goods expiring in next 30 days" with estimated financial impact (quantity × unit cost)
- New movement type: `EXPIRED` for writing off expired stock
- Loss report: total value lost to expiry per warehouse per period

**Data model additions:**
```sql
ALTER TABLE stock_movements ADD COLUMN batch_number VARCHAR(100);
ALTER TABLE stock_movements ADD COLUMN expiry_date DATE;
ALTER TABLE stock_movements ADD COLUMN received_date DATE;
CREATE TABLE expiry_alerts (id, productId, warehouseId, batchNumber, expiryDate, quantity, status, createdAt);
```

---

### 2.2 Theft Detection & Ghost Goods Prevention

**Why:** Reviewer asked how we prevent an employee from bringing outside goods to sell as company goods, and how we detect theft.

**What to build:**

**Ghost goods prevention:**
- Every INTAKE must have an associated approved Purchase Order or Transfer record — no ad-hoc intakes by staff
- INTAKE without a matching PO reference is flagged as an anomaly and requires manager approval before stock is added
- PO reference number is mandatory on all intake movements

**Theft / shrinkage detection:**
- Automated shrinkage alerts: if `system_count - physical_count > configurable_threshold` on reconciliation, auto-flag for investigation
- Staff who repeatedly submit reconciliations showing losses are surfaced in a report
- All movements tied to a `userId` — the audit trail shows who made each edit, when, and from what IP/session
- Row-level audit logging: every `UPDATE` and `DELETE` on stock-sensitive tables is logged to an append-only `audit_log` table

**Audit log table:**
```sql
CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tableName VARCHAR(100),
  recordId BIGINT,
  action ENUM('INSERT', 'UPDATE', 'DELETE'),
  changedBy BIGINT,      -- userId
  changedAt TIMESTAMP,
  before JSON,           -- snapshot before change
  after JSON             -- snapshot after change
);
```

---

### 2.3 Race Condition Handling (Documented & Tested)

**Why:** Reviewer asked how we handle two buyers simultaneously trying to buy from a stock of 24 when they request 10 and 15.

**What is already in v1:** Optimistic locking (`@Version` on `StockLevel`) + `@Retryable` (3 attempts). This already prevents dirty writes.

**What to add in v2:**
- Reservation system: when an order is placed, reserve the quantity atomically using a Redis lock before committing to MySQL
- If reserve fails (stock insufficient), return a `409 CONFLICT` with current available quantity
- Reservation TTL: if payment/confirmation is not received within N minutes, the reservation is released
- Expose `availableStock = currentStock - reservedStock` in all stock-level API responses
- Write integration tests (Testcontainers) that simulate concurrent requests and verify correctness

---

### 2.4 Demand Forecasting — ML-Based (True AI)

**Why:** We described the system as "predictive" in v1, but velocity is a simple moving average, not a forecast. V2 is where real ML enters.

**What to build:**
- Time-series demand forecasting model (Prophet or LSTM) per product per warehouse
- Trained on historical `stock_movement_events` from ClickHouse
- Outputs: predicted demand for next 7, 14, 30 days
- "When to reorder" recommendation: reorder date = today + (days until empty) - lead time (configurable per supplier)
- Confidence interval displayed on dashboard
- Model retrained weekly on new movement data
- Clearly labeled in UI as "AI-Powered Forecast" to distinguish from the rule-based threshold alerts

**Infrastructure:**
- Python microservice (FastAPI) consuming ClickHouse data and exposing `/api/forecast`
- Deployed as a separate Kubernetes pod
- Inventory Service calls forecast endpoint when building dashboard metrics

---

### 2.5 Price Intelligence — When to Buy / When to Sell Cheap

**Why:** Reviewer raised inflation, deflation, global events (wars, pandemics) as unaddressed factors.

**Foundation — capture unit cost on every INTAKE (required for everything below):**
```sql
ALTER TABLE stock_movements ADD COLUMN unit_cost DECIMAL(10,2);
-- Required on INTAKE type, optional on all others
```
Without this field, no price intelligence is possible. It is the single most important migration in v2.

**Layer 1 — Internal Price History (build now, runs on your own data):**
- Every INTAKE records the unit cost paid — this builds a price history per product automatically
- `PriceHistoryService` queries last N intakes per product and computes trend direction (rising / stable / falling)
- **Buy signal:** last 3 intakes show cost increasing >X% → alert Procurement Officer: *"Price rising. Stock up before supplier adjusts further."*
- **Sell cheap signal:** cost falling + stock is high → alert Warehouse Manager: *"Price falling. Consider clearing current inventory before cheaper stock arrives and undercuts your margin."*
- Dashboard widget: price trend chart per product (last 12 months, Recharts line chart)

**Layer 2 — External Market Signals (integrate public APIs, no subscription cost):**

| Signal | API | Data | Cost |
|--------|-----|------|------|
| USD/NGN exchange rate | ExchangeRate-API / CBN public feed | Live exchange rate | Free |
| Crude oil / fuel price | EIA API (US Energy Information) | Daily Brent crude | Free |
| Global commodity prices | World Bank Commodity Price API | Monthly palm oil, wheat, sugar | Free |
| Nigerian fuel pump price | NNPCL public data | PMS price per litre | Free |
| Seasonal calendar | Static config (see 2.5a) | Nigerian holidays, Ramadan, harvest | You define it |

Nightly scheduled job pulls these signals, crosses them against product tags (imported / fuel-sensitive / commodity-linked), and generates forward-looking alerts:

```
"USD/NGN has weakened 8% this month.
 Palm oil on the world market is up 12%.
 Your last 2 intakes of vegetable oil show no price change yet —
 your suppliers likely haven't passed this on yet.
 You have ~22 days of stock.
 Recommended: stock up now before price adjustment hits."
```

**Layer 3 — Event Intelligence (v3 roadmap):**
- News API feeds tagged by event category (conflict, sanctions, weather, policy)
- ML model learns correlations: *"when Russia-Ukraine escalation events occur, flour prices in Lagos historically rise 18% within 3 weeks"*
- Proactive alerts before prices move, not after

**What to say in the presentation:**
> *"In v2, InventAlert watches exchange rates and commodity markets overnight. When the naira weakens or palm oil rises globally, InventAlert alerts your Procurement Officer to restock before your suppliers pass the cost on — not after."*

---

### 2.5a Seasonal Demand Intelligence — Nigerian Market Cycles

**Why:** Nigerian markets are governed by agricultural seasons, not just price trends. Tin tomato demand triples when fresh tomatoes go off-season. Poundo yam flour spikes during the yam lean season. These are predictable, cyclical, and entirely unaddressed by any generic inventory system.

**The economic principle:** Substitute goods. When a substitute product (fresh tomato) becomes unavailable or expensive, demand for your product (tin tomato) rises — same months, every year. The pattern is not random. It is the market calendar.

**Nigerian examples of this dynamic:**

| Scarce / Seasonal Product | Substitute That Spikes | Approximate Peak Window |
|--------------------------|----------------------|------------------------|
| Fresh tomato | Tin tomato, tomato paste | March – September |
| Fresh yam | Poundo yam flour, yam flour | June – August (lean season) |
| Fresh pepper | Dried pepper, pepper mix | Dry season (Nov – Feb) |
| Fresh fish | Frozen fish, stockfish | When artisanal fishing slows |
| Rice (pre-harvest) | Spaghetti, pasta, noodles | July – September |
| Cold weather produce | Preserved / canned equivalents | Harmattan (Nov – Feb) |

**Data model additions:**
```sql
ALTER TABLE products ADD COLUMN seasonal_profile ENUM('standard', 'seasonal_peak', 'seasonal_low', 'substitute_driven') DEFAULT 'standard';
ALTER TABLE products ADD COLUMN peak_start_month TINYINT;    -- e.g., 3 (March)
ALTER TABLE products ADD COLUMN peak_end_month   TINYINT;    -- e.g., 9 (September)
ALTER TABLE products ADD COLUMN demand_multiplier DECIMAL(4,2); -- e.g., 2.5 = 2.5x normal velocity during peak

CREATE TABLE product_substitute_relationships (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id      BIGINT NOT NULL,          -- the product you stock (tin tomato)
  substitute_name VARCHAR(200) NOT NULL,    -- the substitute (fresh tomato — may not be in your catalogue)
  relationship    ENUM('in_season_low', 'off_season_high'),
  companyId       BIGINT NOT NULL
);
-- "when fresh tomato is IN season → tin tomato demand is LOW"
-- "when fresh tomato is OFF season → tin tomato demand is HIGH"
```

**What the intelligence engine does:**

*4 weeks before peak season starts:*
```
"Tin Tomato peak demand season starts in ~4 weeks (March 1).
 Current stock: 320 units | Current velocity: 12 units/day (off-peak)
 Expected peak velocity: 30 units/day (2.5x — historical average)
 Days of stock at peak velocity: 10 days

 Recommended: Order 1,350 units now to cover 45 days of peak demand."
```

*Mid off-season — prices are low:*
```
"Tin Tomato is currently in its low-demand season (fresh tomatoes in harvest).
 Current supplier price: ₦480/unit
 Peak season average price (last 2 years): ₦720/unit
 Potential saving if you stock up now vs. waiting: ₦240/unit × capacity

 You have warehouse space for 800 additional units.
 Buying now saves ~₦192,000 vs. buying during peak."
```

*End of peak — excess stock risk:*
```
"Tin Tomato peak season ends in ~3 weeks (October).
 You have 680 units remaining.
 At off-peak velocity (12/day), those units take 56 days to clear.

 Consider a 10% discount now to move excess stock
 before you're holding high-priced inventory in a low-demand season."
```

**What makes this different from generic price prediction:**
Generic systems react to a price spike after it happens. Seasonal intelligence acts 4 weeks before the spike — because it understands *why* the spike happens every year.

```
Generic system:    "Tin tomato price just increased 40%"       ← too late
InventAlert V2:    "Peak season in 4 weeks — stock up now
                    at today's low price before demand triples" ← ahead of it
```

**Minimum viable demo (buildable before presentation):**
1. Add `peak_start_month`, `peak_end_month`, `demand_multiplier` to Product entity — one migration
2. In the nightly scheduled job: if today is within 4 weeks of `peak_start_month` → generate `SEASONAL_RESTOCK_ALERT`
3. Configure one demo product (tin tomato) with the seasonal profile
4. One alert card on Procurement Officer dashboard showing the projected stock gap and recommended order quantity

That is one migration, one scheduled method, one UI card — fully buildable, and the panel will remember it.

---

### 2.6 Large-Order Blindspot Prevention

**Why:** Reviewer identified: threshold is 500, stock is 700, a customer wants to buy 1,000 — the alert never fires because we haven't crossed the threshold yet, and we lose the sale.

**What to build:**
- **Reservation check at order entry:** Before confirming any sale, check if `requestedQuantity > availableStock`. Return a clear error with current available quantity.
- **Demand spike detection:** If a single order would bring stock from above threshold to zero (or near-zero), auto-trigger a replenishment alert at order entry time, not after the stock has already dropped.
- **Configurable buffer zone:** In addition to the restock threshold, allow a "safety buffer" percentage (e.g., 20%) above threshold. Alerts fire when stock crosses into the buffer zone, giving lead time before the threshold is breached.

---

### 2.7 WhatsApp Notifications for Non-Smartphone Users

**Why:** Target users (e.g., traders in Igbo, Yoruba, Hausa markets) may not use smartphones or email.

**What to build:**
- Integrate WhatsApp Business API (Meta) or Twilio WhatsApp channel
- Each user can opt in to WhatsApp notifications with their phone number
- Critical alerts (stock at threshold, transfer approved, reconciliation rejected) sent via WhatsApp in addition to the app
- Message templates in English initially, then local languages in v3

---

### 2.8 Local Language Support

**Why:** Reviewer noted the system targets non-technical users in Nigerian markets.

**What to build:**
- i18n framework (react-i18next) in the frontend
- Translation files for: English (current), Igbo, Yoruba, Hausa
- Language selector in user profile settings
- Priority strings: dashboard labels, alert messages, error messages, notification text
- WhatsApp message templates in local languages

---

### 2.9 Barcode Scanning

**Why:** Prevents ghost goods — a product without a barcode/SKU in the system cannot be stocked or sold.

**What to build:**
- Frontend: camera-based barcode scanner (react-zxing or QuaggaJS) on mobile browsers
- Backend: barcode field on Product entity, indexed for fast lookup
- INTAKE flow: scan barcode → auto-fill product details → confirm quantity → submit
- Unknown barcode → blocked from intake until product is created in catalogue

---

### 2.10 POS System Integration

**Why:** Sales data should flow directly into InventAlert rather than requiring manual movement entries.

**What to build:**
- Webhook receiver: external POS systems post sale events to `POST /api/integrations/pos/sale`
- API key authentication for POS integrations (separate from JWT user auth)
- Automatic OUTBOUND_SALE movement created from POS event
- Support popular Nigerian POS/payment platforms (e.g., Paystack POS, Flutterwave)
- Idempotency key on all POS webhooks to prevent duplicate stock deductions

---

### 2.11 Sales by Entity — Sales Representative Role, Orders & Payment

**Why:** V1 has no customer-facing sales actor. `OUTBOUND_SALE` is currently just a manual stock deduction typed in by warehouse staff — no customer, no price, no payment record, no order. This section introduces a structured sales flow with a dedicated Sales Representative role.

**Business rule (non-negotiable):**
> **Only approved wholesale/distributor customers ("big customers") can buy on credit. Walk-in and retail customers pay upfront — cash or POS, no exceptions.**

---

#### New Role: Sales Representative

Added to the `Role` enum alongside existing roles. Warehouse-scoped (assigned to one warehouse, same pattern as Warehouse Staff).

| Role | Responsibility |
|------|---------------|
| Company Administrator | Company setup, user management |
| Warehouse Manager | Stock oversight, transfer & reconciliation approvals |
| Warehouse Staff | Physical stock — receive, count, reconcile |
| Procurement Officer | Reorder decisions, supplier management |
| **Sales Representative** ← new | Customer-facing sales, order creation, payment collection |

Sales Representatives see available stock for their assigned warehouse, create sales orders, collect payment, and manage their customer accounts. They cannot approve transfers, reconciliations, or purchase orders.

---

#### Data Model

```sql
-- Customer registry
CREATE TABLE customers (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  companyId       BIGINT NOT NULL,
  name            VARCHAR(200) NOT NULL,
  phone           VARCHAR(20),
  email           VARCHAR(200),
  address         TEXT,
  customerType    ENUM('walk_in', 'retail', 'wholesale', 'distributor'),
  creditEnabled   BOOLEAN DEFAULT FALSE,
  -- creditEnabled = TRUE only for wholesale and distributor types,
  -- set explicitly by Company Administrator. Walk-in and retail are always FALSE.
  creditLimit     DECIMAL(15,2) DEFAULT 0,
  outstandingDebt DECIMAL(15,2) DEFAULT 0,
  paymentTermDays INT DEFAULT 0,             -- e.g., 30 = payment due in 30 days
  createdAt       TIMESTAMP
);

-- Sales order header
CREATE TABLE sales_orders (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouseId     BIGINT NOT NULL,
  customerId      BIGINT,                    -- NULL = walk-in (no account)
  salesRepId      BIGINT NOT NULL,
  status          ENUM('DRAFT','CONFIRMED','PENDING_PAYMENT','PAID','PARTIALLY_PAID','CANCELLED'),
  totalAmount     DECIMAL(15,2),
  amountPaid      DECIMAL(15,2) DEFAULT 0,
  balanceDue      DECIMAL(15,2),
  paymentDueDate  DATE,                      -- set only for credit orders
  createdAt       TIMESTAMP,
  fulfilledAt     TIMESTAMP
);

-- Sales order line items
CREATE TABLE sales_order_items (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  orderId         BIGINT NOT NULL,
  productId       BIGINT NOT NULL,
  quantity        INT NOT NULL,
  unitPrice       DECIMAL(10,2) NOT NULL,    -- selling price at time of sale
  lineTotal       DECIMAL(15,2)
);

-- Payment records (one order can have multiple partial payments)
CREATE TABLE payments (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  orderId         BIGINT NOT NULL,
  amount          DECIMAL(15,2) NOT NULL,
  method          ENUM('CASH','BANK_TRANSFER','POS','CHEQUE'),
  -- CREDIT is not a payment method — it is an order type.
  -- Credit orders have status PENDING_PAYMENT until real payment arrives.
  reference       VARCHAR(200),              -- POS receipt, transfer ref, cheque number
  recordedBy      BIGINT NOT NULL,           -- userId of Sales Rep
  confirmedAt     TIMESTAMP,
  notes           TEXT
);
```

---

#### Sales Flow — End to End

**Walk-in / Retail Customer (pay upfront — no exceptions):**
```
1. Sales Rep selects "Walk-in" or existing retail customer
2. Adds line items (product + quantity)
   → System checks available stock (currentStock - reservedStock)
   → If quantity would cross buffer zone → pre-emptive restock alert fires
3. System shows total amount due
4. Sales Rep collects payment (Cash or POS)
   → Records: method + reference number
5. Payment confirmed → OUTBOUND_SALE movement auto-created per line item
6. Receipt generated
7. Stock updated. Done.
```

**Wholesale / Distributor Customer (credit eligible):**
```
1. Sales Rep selects wholesale/distributor customer
2. Adds line items
   → System checks: outstandingDebt + this order ≤ creditLimit
   → If credit limit would be exceeded → order blocked, Sales Rep alerted
3. Sales Rep chooses: "Pay now" or "Bill on credit"

   PAY NOW path → same as walk-in above
   
   CREDIT path:
     → Order status = PENDING_PAYMENT
     → paymentDueDate = today + paymentTermDays (e.g., 30 days)
     → OUTBOUND_SALE movement created immediately (goods leave warehouse)
     → outstandingDebt += orderTotal on customer record
     → Warehouse Manager notified: "Credit sale to [Customer] for ₦X"

4. Payment arrives (partially or fully) before due date:
   → Sales Rep records payment: method + reference
   → outstandingDebt reduced
   → If fully paid → order status = PAID

5. If unpaid at due date:
   → Day 0 of overdue: alert to Sales Rep
   → Day 7 of overdue: alert escalates to Warehouse Manager
   → Day 14 of overdue: customer's credit suspended
                         no new credit orders until debt is cleared
```

---

#### Credit Rules (Hard-Coded Business Logic)

| Rule | Behaviour |
|------|-----------|
| Walk-in customer | `creditEnabled = FALSE` always. Payment required before goods leave. |
| Retail customer | `creditEnabled = FALSE` always. Payment required before goods leave. |
| Wholesale / Distributor | `creditEnabled` set by Company Administrator. Default FALSE until explicitly enabled. |
| Credit limit | Set per customer by Company Administrator. Sales Rep cannot change it. |
| Credit limit breach | Order blocked at creation. Sales Rep cannot override. Manager cannot override. Only Admin can raise the credit limit. |
| Outstanding debt > 0 at order time | New credit orders blocked until previous debt is cleared or Admin explicitly approves. |
| Credit suspension | Automatic after 14 days overdue. Auto-lifted when full debt is cleared. |

---

#### Payment Methods

| Method | When used | Reference captured |
|--------|----------|--------------------|
| Cash | Walk-in, small retail | None required |
| Bank Transfer | Any customer | Transfer reference number (mandatory) |
| POS | Any customer with card | POS receipt number (mandatory) |
| Cheque | Wholesale/distributor only | Cheque number + bank (mandatory) |

There is no "Credit" payment method. Credit is an order type, not a payment. When a credit customer pays, they use one of the four methods above.

---

#### Gross Margin Visibility (bonus output of this feature)

Because `sales_order_items` captures `unitPrice` (selling price) and `stock_movements` now captures `unit_cost` (intake cost from section 2.5), V2 can compute:

```
Gross Margin per product = unitPrice (sold) - unit_cost (intake)
Gross Margin % = (unitPrice - unit_cost) / unitPrice × 100
```

Surfaced on the Manager dashboard: which products have the healthiest margins, which are being undersold, which cost more to buy than you're recovering on sale.

---

#### Sales Representative Dashboard

One question on landing: *"What can I sell today, and what do my customers owe me?"*

**Panel 1 — Available Stock (this warehouse)**
- Product list with current available quantity (stock - reservations)
- Low-stock warning badge if within buffer zone
- Selling price per unit

**Panel 2 — Outstanding Customer Debts**
- Customer name, amount owed, days overdue
- Colour-coded: green (within terms), amber (due soon), red (overdue)
- One-tap to record a payment against an open order

**Panel 3 — Today's Sales**
- Orders created today, total value, payment status

---

#### How This Connects to the Rest of V2

```
Sales Rep creates order
  → stock reserved (section 2.3 — race condition protection)
  → buffer zone check (section 2.6 — large-order blindspot prevention)
  → credit limit check (this section)

Payment confirmed
  → OUTBOUND_SALE movement auto-created
  → feeds velocity calculation (V1)
  → feeds seasonal demand forecast (section 2.5a)
  → unit_cost vs. unitPrice = gross margin (section 2.5)
  → full audit trail (section 2.2)

Credit order overdue
  → Day 7: Manager alert via WebSocket + email
  → Day 14: credit suspended, WhatsApp alert to Sales Rep (section 2.7)
```

---

#### Minimum Viable Demo (buildable before presentation)

1. Add `SALES_REPRESENTATIVE` to Role enum
2. Three migrations: `customers`, `sales_orders` + `sales_order_items`, `payments`
3. `SalesOrderService` — create order → stock reservation → payment confirmation → OUTBOUND_SALE movement
4. `CustomerService` — credit limit check, outstanding debt update, suspension logic
5. `SalesRepDashboard.jsx` — available stock panel + outstanding debts panel
6. Payment form: method selector + reference field + confirm button

Demo script: Sales Rep sells 50 units of tin tomato to a wholesale distributor on credit → goods leave warehouse → stock updates → 15 days later, payment arrives → debt cleared → credit reinstated.

---

### 2.12 Inline Editing with Field-Level Blame (Spreadsheet UX + Git Blame Accountability)

**Why:** Every reviewer asked how we know who changed what. This answers that question definitively — and simultaneously removes the biggest adoption barrier for non-technical users by making the product feel like a spreadsheet they already know how to use. Excel cannot do this. InventAlert can.

**The feature in one sentence:** Double-click any cell to edit it in place like a spreadsheet. Hover any cell to see who last changed it, when, and what it was before — going all the way back to the record's creation.

---

#### Inline Editing UX

Instead of click → open form → fill fields → save → return to list:

```
Products Table

SKU       Name          Threshold   Unit Cost   
TT-001    Tin Tomato    [  600  ]   ₦480        
                         ↑ user double-clicked
                         → input appears in place
                         → type new value, press Enter
                         → saves immediately
                         → all other users viewing this table see 600 live
```

**Keyboard navigation (true spreadsheet behaviour):**

| Key | Action |
|-----|--------|
| Double-click | Open cell for editing |
| Enter | Save, move to next row same column |
| Tab | Save, move to next column |
| Escape | Cancel, revert to original value |
| Arrow keys | Navigate between cells |

**Validation:**
- Invalid value (negative threshold, non-numeric cost) → cell turns red, tooltip shows error, does not save
- Empty required field → does not save, shows validation message

**What can be edited inline vs. what is read-only:**

| Table | Editable Inline | Read-Only (why) |
|-------|----------------|-----------------|
| Products | name, threshold, unit cost, unit of measure | SKU, id, created date |
| Stock Levels | threshold per warehouse | current stock — only changes via recorded movements or approved reconciliation |
| Customers | name, phone, credit limit, payment term days | outstanding debt, customerType — debt is system-computed, type set at creation |
| Warehouses | name, address | lat/lng — set via Google Maps autocomplete only |

**Current stock is never directly editable inline — hard rule.** Editing raw stock numbers would bypass the movement audit trail and create a fraud vector. Stock only changes through INTAKE, OUTBOUND\_SALE, TRANSFER, or approved RECONCILIATION.

---

#### Field-Level Change Log (the git blame data store)

The existing `audit_log` from section 2.2 stores full JSON row snapshots (before/after). That tells you *something* changed — but you have to diff the JSON to find *what*.

V2 adds a dedicated field-level table — one row per field changed:

```sql
CREATE TABLE field_change_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tableName   VARCHAR(100) NOT NULL,    -- 'products', 'customers', 'warehouses'
  recordId    BIGINT       NOT NULL,    -- the product/customer/warehouse id
  fieldName   VARCHAR(100) NOT NULL,    -- 'threshold', 'unitCost', 'name'
  oldValue    TEXT,                     -- value before the change
  newValue    TEXT,                     -- value after the change
  changedBy   BIGINT       NOT NULL,    -- userId
  changedAt   TIMESTAMP    NOT NULL,
  sessionId   VARCHAR(100)              -- groups fields changed in the same edit action
);

CREATE INDEX idx_field_blame ON field_change_log (tableName, recordId, fieldName, changedAt DESC);
```

When a user saves a cell, the backend diffs old value vs. new value and writes exactly one row per changed field. If only `threshold` changes, one row. If `name` and `unitCost` both change in the same edit, two rows — each independently queryable.

**Implementation:** Spring AOP aspect on all service-layer update methods. Intercepts the call, reads the current entity state before the save, compares after, writes the diff to `field_change_log`. Zero boilerplate in controllers or services.

---

#### The Blame Tooltip

Hover over any edited cell:

```
┌─────────────────────────────────────────────────────┐
│ threshold                                           │
├─────────────────────────────────────────────────────┤
│ 600   Chidi Okeke  ·  May 15, 2026  ·  2:34 PM     │  ← current
│ 500   Amaka Eze    ·  May 10, 2026  ·  9:17 AM     │
│ 400   System       ·  Apr  2, 2026  ·  initial     │  ← original
└─────────────────────────────────────────────────────┘
                                 [View full history →]
```

API: `GET /api/audit/field-history?table=products&recordId=123&field=threshold`

Fast lookup via the index on `(tableName, recordId, fieldName, changedAt DESC)`. Returns last N entries for that specific field on that specific record. Only shows the tooltip if at least one change exists — virgin fields show no tooltip.

---

#### Full Record History Modal

"View full history" opens a `git log`-style timeline for the entire record — every field ever changed, by anyone, oldest to newest:

```
May 15 · 2:34 PM  Chidi Okeke (Warehouse Manager)
  threshold:  500  →  600

May 10 · 9:17 AM  Amaka Eze (Company Administrator)
  threshold:  400  →  500
  unitCost:   ₦450 →  ₦480

Apr 12 · 11:02 AM  John Nwosu (Company Administrator)
  name:       "Tomatopaste"  →  "Tin Tomato"

Apr 2 · 8:00 AM   System  (record created)
  All fields initialised.
```

API: `GET /api/audit/record-history?table=products&recordId=123`

---

#### Real-Time Collaboration Awareness

Because WebSocket (STOMP) infrastructure already exists, edits can be broadcast live to all users viewing the same table:

```
User A (Lagos) saves threshold 500 → 600
  → Backend writes to DB
  → Publishes Kafka event: field.changed { table, recordId, field, newValue, changedBy, changedAt }
  → NotificationService broadcasts via WebSocket to all connected users viewing this product list
  → User B (Abuja) sees cell flash and update to 600 in real time
  → Small avatar chip appears on the cell: "Chidi just edited"
```

No page refresh. No stale data. Same collaborative feel as Google Sheets — but with full accountability that Google Sheets does not provide.

---

#### Conflict Resolution

If two users edit the same cell within seconds of each other:

```
User A saves threshold → 600  at 2:34:01 PM
User B saves threshold → 550  at 2:34:03 PM  (server receives this 2 seconds later)

User B sees:
┌──────────────────────────────────────────────────────────┐
│ ⚠ Edit conflict                                          │
│ Chidi Okeke changed this field to 600 (2 seconds ago)    │
│ Your value: 550                                          │
│                                                          │
│  [Keep 600 — Chidi's value]   [Save 550 — my value]     │
└──────────────────────────────────────────────────────────┘
```

Either choice is valid and recorded. The resolution itself writes a `field_change_log` entry with the actor who made the final call. No silent overwrites — the person who was overridden can always see it happened.

---

#### How This Connects to the Rest of V2

```
User edits threshold inline
  → Spring AOP aspect intercepts the save
  → Diffs old vs. new value
  → Writes to field_change_log (this section)
  → Writes to audit_log row snapshot (section 2.2)
  → Publishes field.changed Kafka event
  → WebSocket broadcasts to all viewers (existing infra)
  → ThresholdCheckService re-evaluates alert status with new threshold
  → If new threshold now breached → alert fires immediately
```

---

#### Minimum Viable Demo (buildable before presentation)

1. `field_change_log` table — one migration
2. Spring AOP `FieldAuditAspect` — intercepts all `ProductService.update()`, `CustomerService.update()`, etc. — diffs and writes log
3. `GET /api/audit/field-history` endpoint
4. `GET /api/audit/record-history` endpoint
5. `EditableCell.jsx` — double-click activates inline input, Enter saves via RTK mutation, Escape reverts
6. `BlameTooltip.jsx` — fetches and renders last 3 changes on hover
7. `FieldHistoryModal.jsx` — full timeline for a record
8. WebSocket listener: `field.changed` events update cells and show editor chip

Demo script: open Products table → double-click threshold on Tin Tomato → change 500 to 600 → press Enter → open blame tooltip → show "Chidi changed this from 500 to 600 at 2:34 PM" → open full history → show every change since record creation.

---

#### The Line That Wins the Room

> *"Every reviewer asked: when using the spreadsheet, how do you know who changed what? With InventAlert, you hover over any number in the system and see exactly who changed it, when, and what it was before — going all the way back to when the record was created. That is git blame for your inventory data. Excel has never done this and never will."*

---

## Priority 3 — Infrastructure & Engineering

### 3.1 CI/CD Improvements
- Add integration test stage (Testcontainers with real MySQL, Redis, Kafka) to CI pipeline
- Add code coverage threshold gate (minimum 70%)
- Slack/email notification on deployment failure
- Blue-green deployment strategy (zero-downtime deploys on EKS)

### 3.2 Kubernetes Auto-Scaling
- HorizontalPodAutoscaler on all 4 services (scale on CPU > 70% or RPS threshold)
- Minimum 2 replicas for all services in production (no single point of failure)
- PodDisruptionBudget to prevent all pods from being evicted simultaneously

### 3.3 NDPA Compliance (Nigeria Data Protection Act)
- Data Processing Agreement documentation
- Right to erasure: `DELETE /api/companies/{id}` triggers full data wipe across all schemas
- Data retention policy: configurable per company, default 5 years
- Audit log for all PII access
- Privacy policy and terms of service pages in the frontend
- Cookie consent if applicable

### 3.4 Row-Level Audit Logging
- Append-only `audit_log` table in every company schema
- Spring AOP aspect that intercepts all service-layer writes and logs before/after snapshots
- Queryable via Manager/Admin dashboard: "Who changed what, when?"

---

## V2 Delivery Plan (Sprint Breakdown)

### Sprint 1 (Bugs + Critical Fixes)
- Fix transfer approval flow (destination manager first)
- Fix Procurement Officer warehouse scope
- Fix manager update UI
- Add cursor pagination on movements
- Add alert filtering by warehouse
- Add purchase order audit trail

### Sprint 2 (Safety & Integrity)
- Batch/lot tracking + expiry alerts
- Ghost goods prevention (PO-linked intakes)
- Race condition reservation system
- Row-level audit logging (audit_log table + Spring AOP aspect)
- Field-level change log (field_change_log table + FieldAuditAspect)
- Inline cell editing UI (EditableCell component + keyboard navigation)
- Blame tooltip + full record history modal
- WebSocket broadcast of field.changed events (real-time collaboration)

### Sprint 3 (Intelligence)
- Add `unit_cost` field to INTAKE movements (foundation for all price features)
- Internal price history tracking + buy/sell trend signals
- External market signal integration (USD/NGN rate, commodity prices — free APIs)
- Seasonal demand intelligence: product seasonal profiles + substitute relationships
- Seasonal restock alerts (4-week pre-peak warnings)
- Large-order blindspot prevention (buffer zone alerts)
- ML demand forecasting service (Python/FastAPI)
- NDPA compliance documentation + data deletion endpoint

### Sprint 4 (Sales, Inclusivity & Integrations)
- Sales Representative role + customer registry
- Sales order flow (walk-in vs. credit customer paths)
- Payment recording (Cash, Bank Transfer, POS, Cheque)
- Credit limit enforcement + debt tracking + overdue escalation
- Sales Rep dashboard (available stock + outstanding debts)
- WhatsApp notifications (Twilio)
- Local language support (react-i18next)
- Barcode scanning (frontend)
- POS webhook integration (auto-creates OUTBOUND_SALE on payment)
- CI/CD blue-green deployment

---

## V2 Architecture Changes

```
V1 Architecture:
  identityService → Kafka → inventoryService → Kafka → notificationService
                                            → Kafka → analyticsService

V2 Additions:
  + forecastService (Python/FastAPI) ← ClickHouse historical movements
  + priceIntelligenceService (Spring Boot module in inventoryService)
      - internal: unit_cost on INTAKE → price history → trend detection
      - external: nightly job pulling USD/NGN, crude oil, commodity APIs
      - signals: BUY_NOW / HOLD / SELL_CHEAP alerts to Procurement Officer
  + fieldAuditService (Spring AOP aspect across all services)
      - field_change_log table (one row per field changed)
      - GET /api/audit/field-history (blame tooltip data)
      - GET /api/audit/record-history (full git-log style timeline)
      - field.changed Kafka event → WebSocket broadcast to table viewers
  + EditableCell.jsx + BlameTooltip.jsx + FieldHistoryModal.jsx (frontend)
  + salesService (Spring Boot module in inventoryService)
      - SalesRepresentative role (warehouse-scoped)
      - customers table (walk-in, retail, wholesale, distributor)
      - sales_orders + sales_order_items + payments tables
      - credit limit enforcement (wholesale/distributor only)
      - debt tracking + overdue escalation (day 7 → manager, day 14 → suspend)
      - OUTBOUND_SALE auto-created on payment confirmation
      - gross margin calculation (unitPrice vs. unit_cost)
  + seasonalIntelligenceService (Spring Boot module in inventoryService)
      - product seasonal profiles (peak_start_month, peak_end_month, demand_multiplier)
      - substitute goods relationships (product_substitute_relationships table)
      - nightly job: 4-week pre-peak restock alerts + off-season sell-off alerts
  + Twilio WhatsApp integration (in notificationService)
  + Barcode lookup endpoint (in inventoryService)
  + POS webhook receiver (new endpoint in inventoryService)
  + HorizontalPodAutoscaler on all Kubernetes deployments
  + Blue-green deployment pipeline (GitHub Actions)
```

---

## V2 Success Metrics

| Metric | V1 Baseline | V2 Target |
|--------|------------|-----------|
| Transfer approval accuracy | Bug present | Destination-first flow, 0 errors |
| Stock-out incidents | Not tracked | 30% reduction via buffer zone alerts |
| Reconciliation discrepancies caught | Manual | 100% of discrepancies audited with actor |
| Race condition errors (concurrent orders) | Theoretical risk | 0 dirty writes under concurrent load |
| Alert delivery latency (WebSocket) | ~1s | < 500ms |
| Forecast accuracy (v2 ML) | N/A | MAPE < 15% on 7-day horizon |
| Field-level change tracking | 0 | Every field edit attributed to a user with old/new value |
| Inline editing coverage | 0 | Products, customers, warehouses all inline-editable |
| Conflict resolution | 0 | Detected and surfaced — no silent overwrites |
| Credit sales tracked | 0 | Full debt lifecycle per wholesale/distributor customer |
| Walk-in cash enforcement | Not enforced | 100% — no credit possible for walk-in/retail |
| Overdue debt escalation | 0 | Auto-alert Day 7, auto-suspend Day 14 |
| Gross margin visibility | 0 | Per product, per warehouse on Manager dashboard |
| Pre-peak restock alerts fired | 0 | ≥4 weeks before every seasonal peak |
| Procurement decisions informed by price signals | 0 | Exchange rate + commodity signals live |
| Sell-off alerts before season end | 0 | Excess stock flagged ≥3 weeks before off-season |
| Non-smartphone user reach | 0 | WhatsApp alerts live |
| NDPA compliance | Not assessed | Documented and implemented |
