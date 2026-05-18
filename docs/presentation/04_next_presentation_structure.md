# Next Presentation Structure

**Format:** 3 parts, as instructed by Rahul  
**Target duration:** ~20–25 minutes + Q&A  
**Prep required:** Diagrams, fallback screenshots/video, competitor analysis table, NDPA slide

---

## Before You Begin — Prep Checklist

- [ ] 60-second elevator pitch rehearsed (timed)
- [ ] Fallback screenshots of every demo step ready
- [ ] Pre-recorded short demo clip (~3 min) as backup if live demo fails
- [ ] All 5 diagrams prepared and accessible:
  - Use Case Diagram (who does what)
  - System/Component Diagram (microservices + data flow)
  - Sequence Diagram: stock alert → transfer suggestion flow
  - Sequence Diagram: reconciliation workflow
  - ERD (entity-relationship diagram)
- [ ] Competitor analysis table prepared (for Q&A slide, not main deck)
- [ ] Manager update UI bug fixed before demo
- [ ] Transfer approval flow bug fixed before demo
- [ ] Slides checked for legibility (font ≥ 24pt, high contrast)
- [ ] Speak with at least one real warehouse manager or procurement officer before the presentation

---

## HOOK — 60-Second Elevator Pitch

> **Open with the persona, not the product.**

*"This is Chidi. He runs three warehouses across Lagos and Abuja for a distributor that moves fast-moving consumer goods. Every week, his team loses sales because the Abuja warehouse runs out of goods that are sitting unused in Lagos. Every month, his accountant flags stock discrepancies that no one can explain — missing goods, unexplained shortfalls, or products that expired on the shelf. Chidi tracks all of this in a shared Excel file that three people edit at the same time.*

*InventAlert replaces that Excel file with a real-time, multi-warehouse inventory intelligence system — so Chidi's team knows what's running low before it runs out, moves stock between warehouses before placing an external order, and has a complete audit trail that tells them exactly who changed what, and when."*

**Why this works:** Names the user, the problem, and the solution in under 60 seconds. The audience knows what we built before the first slide.

---

## Part 1 — The Product (8–10 minutes)

### Slide 1: The Problem (1 min)
**Title:** "The real cost of manual inventory tracking"

Three concrete pain points — use numbers where possible:
1. **Stockouts** — lost sales when one warehouse has nothing but another has surplus
2. **Shrinkage** — theft, ghost goods, unexplained losses with no audit trail to investigate
3. **Expiry losses** — goods that expire on the shelf because nobody tracked the dates

**Key line:** "The problem isn't that Chidi doesn't know his inventory is wrong. The problem is he finds out too late to do anything about it."

---

### Slide 2: The Solution (30 sec)
**Title:** "InventAlert — Real-time stock intelligence for multi-warehouse businesses"

One sentence: *"InventAlert gives warehouse managers, procurement officers, and company admins a shared, real-time view of stock across all their locations — with automatic alerts, intelligent transfer suggestions, and a full audit trail."*

**USP (say this out loud):** "Unlike a spreadsheet, InventAlert tells you what's about to run out *before* it does — and suggests moving stock from where it's sitting unused before you spend money on a new order."

---

### Slide 3: Who Uses It (1 min)
**Title:** "Four roles, four views, one shared truth"

| Role | What they see |
|------|--------------|
| Company Administrator | Full company overview, user management, all warehouses |
| Warehouse Manager | Their warehouse — current stock, incoming/outgoing transfers, reconciliation approvals |
| Warehouse Staff | Their warehouse — record movements, submit stock counts |
| Procurement Officer | Their warehouse — restock alerts, purchase decisions, transfer vs. buy analysis |

**Correction from v1:** Do not say "Admin." Say "Company Administrator." The panel noticed generic role names.

---

### Slide 4 + Demo: The Core Workflow (5–7 min)

**Demo the golden path — practice this until it takes under 6 minutes.**

**Step 1:** Stock falls below threshold in Warehouse A (Abuja)  
→ System checks: is there surplus anywhere nearby?  
→ Yes — Warehouse B (Lagos) has surplus  
→ Transfer suggestion created

**Step 2:** Manager of Warehouse B (Lagos, destination) receives the request  
→ Approves it  
→ Now Warehouse A sees the alert and transfer suggestion  
→ Procurement Officer (Warehouse A) is notified: "Internal transfer available — do you still need to place a purchase order?"

**Step 3:** Warehouse Staff records TRANSFER_OUT from Lagos  
→ Staff at Abuja records TRANSFER_IN  
→ Stock levels update in real time  
→ WebSocket notification arrives instantly

**Step 4:** Monthly reconciliation  
→ Staff submits physical count discrepancy  
→ Manager reviews, approves  
→ Stock corrects, audit trail records who did what

**If live demo fails:** Show the pre-recorded video or screenshots. Do not spend more than 1 minute troubleshooting live.

---

## Part 2 — Technical Decision-Making (7–8 minutes)

### Slide 5: Architecture Overview (2 min)
**Title:** "Four microservices, one event-driven backbone"

Show the system/component diagram. Walk through the data flow in one sentence per service:

1. **Identity Service** — who you are and what you're allowed to do
2. **Inventory Service** — the core business logic (stock, movements, alerts, transfers, reconciliation)
3. **Notification Service** — real-time delivery of every event to the right person
4. **Analytics Service** — append-only event store for trend analysis and dashboards

**Key line:** "Services don't call each other directly. They publish events to Kafka. This means each service can fail independently without taking down the whole system."

---

### Slide 6: Technical Highlight #1 — Multi-Tenancy (1.5 min)
**Title:** "How we keep company data completely isolated"

**Decision:** Per-company MySQL schema (`company_<companyId>`) rather than shared tables with a `companyId` column.

**Trade-off we made:**
- Shared table: simpler, fewer schemas, easier to query across companies
- Per-schema: complete data isolation, easier to delete a company's data, tenant cannot accidentally see another's data even with a query bug

**Why this matters for InventAlert:** B2B SaaS with multiple competing businesses using the same platform. Data isolation is non-negotiable.

---

### Slide 7: Technical Highlight #2 — Race Conditions & Stock Integrity (1.5 min)
**Title:** "How we handle two buyers reaching for the same last unit"

**The problem:** Two users simultaneously submit an OUTBOUND_SALE. Stock is 24. One requests 10, one requests 15. Without protection, both succeed and stock goes to -1.

**Our solution:**
- Optimistic locking: `@Version` column on `StockLevel` — first write wins, second gets a conflict exception
- `@Retryable` (3 attempts): the losing transaction retries, by which time the stock has updated and it correctly fails with "insufficient stock"
- V2 addition: Redis-based reservation before the database write, so the check is atomic

**Why optimistic over pessimistic locking:** Pessimistic locks block all readers. For a high-read inventory system, this would kill performance. Optimistic locking only blocks on write conflicts — rare in normal operation.

---

### Slide 8: Technical Highlight #3 — Transfer Intelligence (2 min)
**Title:** "Why we suggest a transfer before raising a purchase order"

**The algorithm:**
1. Stock drops below configured threshold
2. Before creating a restock alert (external purchase), check every other company warehouse for surplus
3. Use Google Maps Distance Matrix API to find the nearest surplus warehouse (Haversine as fallback)
4. If within 200 km (configurable): suggest an internal transfer — no external spend required
5. Only if no internal option exists: create a restock alert for Procurement

**Business impact:** A single prevented external purchase order on a high-value product can save more than the cost of running the system for a month.

**Distance cap (200 km) is configurable per company** — a company with local warehouses uses 50 km; a national distributor uses 500 km.

---

### Slide 9: What "AI" Means in InventAlert (1 min)
**Title:** "What's heuristic, what's data-driven, and what's coming"

Be precise — the panel will probe this:

| Feature | Type | V1/V2 |
|---------|------|-------|
| Restock alerts | Rule-based (threshold breach) | V1 |
| Transfer suggestions | Distance heuristic (nearest surplus) | V1 |
| Velocity / days to empty | Statistical (moving average) | V1 |
| Demand forecasting | ML time-series model (Prophet/LSTM) | V2 roadmap |
| Price intelligence | Trend detection on historical prices | V2 roadmap |

**Key line:** "We do not call the threshold logic 'AI.' The demand forecasting model in v2 is the first true ML component."

---

## Part 3 — Next Steps (4–5 minutes)

### Slide 10: What We Fixed in V2 (1 min)
**Title:** "V1 bugs we've already resolved"

| Bug | V1 | V2 |
|-----|----|----|
| Transfer approval order | Source warehouse saw suggestion first | Destination manager approves first |
| Procurement Officer scope | Company-wide | Warehouse-scoped |
| Movements pagination | All records returned | Cursor-based, 20/page |
| Alert filtering | No warehouse filter | Filterable by warehouse |

---

### Slide 11: V2 Roadmap (2 min)
**Title:** "V2 — Smarter, Safer, More Inclusive"

Group into three themes:

**Smarter:**
- ML demand forecasting (Prophet/LSTM on historical movements)
- Price intelligence — when to buy, when to sell cheap
- Large-order blindspot prevention (buffer zone alerts)

**Safer:**
- Batch/lot tracking with expiry dates and expiry loss reports
- Ghost goods prevention (PO-linked intakes only)
- Row-level audit logging (who changed what, when, with before/after snapshots)
- Race condition reservation system (Redis atomic reservations)

**More Inclusive:**
- WhatsApp notifications for non-smartphone users
- Local language support: Igbo, Yoruba, Hausa
- Barcode scanning on mobile browser
- POS system integration (Paystack POS, Flutterwave)

---

### Slide 12: Business Impact & Scalability (1 min)
**Title:** "Built to scale with the business"

**Now:**
- Single-tenant or multi-tenant SaaS
- Handles any number of warehouses per company
- Kubernetes auto-scaling on Amazon EKS — add pods under load

**V2 scalability:**
- Horizontal Pod Autoscaler on all 4 services (scales on CPU/RPS)
- Blue-green deployments (zero-downtime updates)
- ClickHouse handles billions of movement events without degradation

**Business impact (target metrics):**
- 30% reduction in stock-out incidents via buffer zone alerts
- Measurable reduction in external purchase orders via internal transfer optimization
- 100% audit coverage: every stock change linked to a user and timestamp

---

### Slide 13: Regulation — NDPA (30 sec)
**Title:** "Data Privacy — NDPA Compliance"

*"InventAlert stores employee names, emails, and operational data. This falls under the Nigeria Data Protection Act (NDPA). Our v2 compliance plan includes: data retention policies, right-to-erasure endpoint, audit logs for PII access, and a documented data processing agreement."*

Do not skip this. The panel flagged it for all teams.

---

## Q&A — Slides to Have Ready (Not Presented, Available on Demand)

1. **Competitor analysis table:**
   - Excel/Google Sheets vs. Zoho Inventory vs. Odoo vs. InventAlert
   - Columns: multi-warehouse, real-time alerts, transfer suggestions, reconciliation, African market pricing, offline support, local language

2. **All 5 diagrams:** Use Case, Component, Sequence (alert flow), Sequence (reconciliation), ERD

3. **Configurable thresholds demo:** Show exactly how a Manager sets/changes a product threshold in the UI

4. **Edge case answers (prepared, not improvised):**

   | Question | Prepared Answer |
   |----------|----------------|
   | "What if a customer wants to buy more than you have in stock?" | Reservation check at order entry returns current available quantity. Buffer zone alert fires when order would deplete stock below threshold. |
   | "How do you prevent theft?" | Row-level audit log records every change with userId and timestamp. PO-linked intakes prevent ghost goods. Reconciliation discrepancy alerts flag repeated shortfalls. |
   | "Two buyers at the same time?" | Optimistic locking + @Retryable. V2 adds Redis atomic reservation. First valid write wins; second gets a clear "insufficient stock" error. |
   | "What about users who don't have smartphones?" | V2: WhatsApp notifications via Twilio. V3: local language SMS. |
   | "Is this AI?" | Threshold alerts are rule-based. Velocity is a moving average. V2 ML demand forecasting is where the first real model enters. |
   | "Which users did you validate with?" | Be honest. State who you spoke with, or acknowledge this as a gap and describe the plan to engage real users. |

---

## Timing Guide

| Section | Time |
|---------|------|
| Hook (elevator pitch) | 1 min |
| Part 1: Product (problem + solution + USP + demo) | 9 min |
| Part 2: Technical decisions (3 highlights + AI framing) | 8 min |
| Part 3: Next steps (V2 fixes + roadmap + impact + NDPA) | 5 min |
| Q&A buffer | 5–7 min |
| **Total** | **~28 min** |

---

## Presenter Notes

- **Open with Chidi.** Do not open with "Hi, we built an inventory management system."
- **State the USP in the first 90 seconds.** "Unlike Excel, InventAlert alerts you before the stock runs out — not after."
- **Use domain role names.** Never say "Admin." Say "Warehouse Manager" or "Procurement Officer."
- **When the AI question comes:** pause, nod, and give the prepared table answer. Do not improvise.
- **If the demo breaks:** immediately switch to the fallback video. Do not spend more than 30 seconds on a live failure.
- **Reconciliation = fraud prevention.** Make this explicit. It resonates with the business audience.
- **NDPA slide is not optional.** Mention it even if briefly — the panel is watching.
