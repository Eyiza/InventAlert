# Feedback & Corrections — Post-Presentation Notes

**Date received:** 2026-05-15  
**Source:** Rahul (Lead Reviewer) + General Capstone Review Panel

---

## From Rahul (Structural Feedback)

### Presentation Structure
Rahul's recommended flow — which we did not fully follow:

1. **Elevator pitch first** (30–60 seconds max)
2. Problem statement — state the user need clearly
3. Solution overview — one sentence: approach + why it fits
4. Architecture & key components — system diagram, data flow, critical modules
5. Technical highlights — 2–3 decisions/trade-offs/algorithms that matter most
6. Roadmap & impact — next steps, scalability, user/business impact

**What we did wrong:** The demo did not open with a crisp elevator pitch. The audience had to wait too long before understanding what problem we were solving.

### Demo Structure
Split the demo into exactly **3 parts**:
- Part 1: The product — how it makes lives easier (user-facing value)
- Part 2: Technical decision-making — why we built it this way
- Part 3: Next steps — roadmap and what comes after v1

**What we did wrong:** We blended all three parts together, making it hard for the audience to follow the narrative thread.

### Practice & Fallback
> "Practice to ensure the demo doesn't overrun; have fallback screenshots or recorded short clips in case of live-demo failures."

We need a pre-recorded walkthrough video and screenshots as backup. Live demos can fail.

---

## General Capstone Panel Feedback

### 1. Engage Professionals Early
We did not validate the solution with actual warehouse managers, procurement officers, or inventory staff before building. The panel asked: **"Which of these users did you engage in understanding and then designing the solution to the problem?"** We had no clear answer.

**Correction:** Before the next presentation, get at least one real user in the target domain to review the product or participate in a demo session.

### 2. Slide Legibility
Slides were not clearly visible or legible to all audience members. Text was too small, contrast was poor, or slides were too information-dense.

**Correction:** Increase font size, reduce slide density, use high-contrast visuals.

### 3. USP Not Prominent Enough
The Unique Selling Proposition was buried in the demo. Reviewers had to ask what made InventAlert different.

**Correction:** State the USP in the first 60 seconds. Do not make the audience search for it.

### 4. Dashboard Design Principle
> "When building dashboards, lead with a core question: What does this user need to see the moment they land here?"

Our dashboards showed too many things without a clear visual hierarchy.

**Correction:** For each role dashboard, define the #1 question that role needs answered on landing. Everything else is secondary.

### 5. Diagrams Not Ready
The panel asked for architecture, use case, and sequence diagrams that we could not quickly present.

**Correction:** Have these five diagrams ready and immediately accessible:
- Use Case Diagram (who does what)
- System/Component Diagram (how components interact)
- Sequence Diagram (stock alert flow, transfer approval flow)
- Activity Flow (reconciliation workflow)
- ERD (entity relationships)

### 6. Problem Framing Was Unclear
> "If your audience has to ask you to re-explain the problem, that is a signal the initial framing was not clear enough."

We had a question mid-demo asking us to re-clarify what exact problem we were solving.

**Correction:** Open with a one-sentence problem statement that any non-technical person understands. Test it on someone outside the project.

### 7. AI Framing
> "Be intentional about mentioning AI. The reaction varies — some question whether you built anything of substance."

We did not have ML/AI in v1 but mentioned "predictive" features in a way that overpromised.

**Correction:** Be precise. Do not use the word "AI" for rule-based threshold logic. Reserve it for the v2 ML forecasting roadmap, and be explicit about what is heuristic vs. model-based.

### 8. Role Names — Use Domain Language
> "Use precise role names, not 'Admin.'"

We used "Admin" generically in several places.

**Correction:** Use exact domain role names everywhere: **Warehouse Manager**, **Procurement Officer**, **Warehouse Staff**, **Company Administrator**, **Platform Super-Admin**.

---

## Technical Feedback (Specific to InventAlert)

### From Presentation Recording / Reviewer Notes

1. **Manager role update UI looks broken** — something in the update flow for manager-level users appears visually or functionally broken. Must fix before v2 demo.

2. **Transfer Suggestion flow is backwards** — Current behavior: source warehouse gets the transfer suggestion first. Correct behavior:
   - When a transfer is suggested, the **manager of the destination warehouse** must approve it first
   - Only after destination approval does it appear as an alert/suggestion for the source warehouse
   - Procurement Officer is then looped in to decide: buy externally or use the internal transfer?

3. **Procurement Officers are company-wide, not warehouse-scoped** — this is wrong. A Procurement Officer should be assigned to a specific warehouse, not the whole company.

4. **Alerts not filtered by warehouse** — Alert frequency / display should be filterable and grouped by warehouse.

5. **Cursor pagination missing on product movements** — Movements list should be paginated at 20 per page using cursor-based pagination.

6. **Purchase order audit trail** — need clear audit log for all purchase order events.

---

## Business/Domain Edge Cases the Panel Raised

These are unsolved problems the panel expects us to address:

| Edge Case | Question Raised |
|-----------|----------------|
| Threshold blindspot | Threshold is 500, stock is 700, someone wants to buy 1,000 — alert never fires and we lose the sale. How do you prevent this? |
| Race conditions | Two buyers simultaneously trying to purchase from a stock of 24 (10 + 15). How is this handled? |
| Ghost goods / fraud | An employee brings outside goods to sell in the shop as company goods, reducing company product sales. How do you detect this? |
| Expiry losses | Products expire before being used. How is expiry loss tracked? |
| Price volatility | Inflation, deflation, global events (wars, pandemics) affect when to buy or sell cheap. Is this considered? |
| Non-smartphone users | Target users (e.g., traders in Igbo markets) may not have smartphones. How do they get alerts? |
| Regulation (NDPA) | Nigeria Data Protection Act applies to all user data. Are we compliant? |
| Configurable thresholds | Reviewers asked for a clear demo of how thresholds are set and changed per product per warehouse. |
| AI vs. heuristic distinction | Reviewers asked to clarify which features are truly AI-driven vs. rule-based. |
| Target market clarity | Warehouse-to-warehouse business vs. warehouse-to-end-user — which are we solving for? |

---

## Competitor Analysis
The panel recommended having a competitor analysis table ready for Q&A (not necessarily in the main slides). Honest comparison against:
- Excel/Google Sheets (the status quo)
- Zoho Inventory
- Odoo
- TradeGecko / QuickBooks Commerce
- Local Nigerian alternatives

Format: feature comparison table showing what is available **now in v1** vs. **roadmapped for v2**.

---

## Summary of Corrections Before Next Presentation

| # | Correction | Priority |
|---|-----------|----------|
| 1 | Fix transfer suggestion approval flow (destination manager approves first) | Critical |
| 2 | Fix Procurement Officer scope — warehouse-level, not company-level | Critical |
| 3 | Fix manager update UI bug | Critical |
| 4 | Add cursor pagination (20/page) on product movements | High |
| 5 | Add alert filtering by warehouse | High |
| 6 | Add purchase order audit trail | High |
| 7 | Open presentation with 60-second elevator pitch | High |
| 8 | Prepare all 5 diagrams (use case, component, sequence, activity, ERD) | High |
| 9 | Prepare fallback screenshots / recorded demo video | High |
| 10 | Address NDPA compliance in presentation | Medium |
| 11 | Prepare competitor analysis table for Q&A | Medium |
| 12 | Clarify AI vs. heuristic language throughout | Medium |
| 13 | User validation — speak to at least one real procurement/warehouse professional | Medium |
