 Your USP

  One sentence: InventAlert moves your own stock between warehouses before it spends your money on a new order.

  That's it. Every competitor (Excel, Zoho, Odoo) tells you when you're low. Only InventAlert asks "do you already own the solution?" — calculates the nearest warehouse with
  surplus, routes a transfer suggestion, and only escalates to a purchase order when no internal option exists. That is the entire defensible edge.

  ---
  What Is Broken / Incomplete That Can Cost You the Win

  Here is the honest flow, in order of damage:

  ---
  🔴 Critical — Breaks the USP Live in Demo

  1. Transfer approval flow is inverted
  Your signature feature doesn't work correctly. Right now the source warehouse sees the suggestion before the destination manager has agreed to send anything. That's logically
  broken — you're telling Abuja "stock is coming from Lagos" before Lagos agreed.

  Correct flow:
  Stock drops below threshold (Abuja)
    → System finds surplus in Lagos
    → Lagos Manager gets request: "Can you send?"
    → Lagos Manager approves
    → ONLY NOW: Abuja sees the alert + transfer suggestion
    → Procurement Officer is notified: "internal transfer available, hold the PO"
  If a reviewer traces this flow in the demo and catches the inversion, your USP falls apart in front of them.

  2. Procurement Officers are company-wide, not warehouse-scoped
  A core design principle you stated — role-based, warehouse-aware access — is violated in your own data model. The reviewer will ask "which warehouse does this Procurement
  Officer belong to?" and the answer will be "all of them, incorrectly."

  ---
  🟠 High — Directly Asked About by Reviewers, Still Missing

  3. No alert filtering by warehouse
  You support multi-warehouse. But every manager sees every alert for every warehouse. The panel noticed this creates noise and defeats the "right person, right alert" promise.

  4. No cursor pagination on movements
  They asked for it. It's not there. A high-volume warehouse will timeout or flood the UI. Simple to add, embarrassing to be missing.

  5. No purchase order audit trail
  Reviewers asked "can we see the full history of a purchase decision?" The answer is currently no — there's movement history but no dedicated PO event log.

  6. Manager update UI is broken
  Visual or functional bug on the manager update form. Anything broken in a live demo costs you credibility instantly.

  ---
  🟡 Medium — Will Come Up in Q&A, No Answer Ready

  7. No expiry/batch tracking
  Reviewer explicitly asked about loss due to expiration. You have no answer in the product today. You need at minimum a roadmap slide that shows you understand the problem
  deeply.

  8. "AI" overclaim
  You said "predictive" in v1. Your velocity calculation is a moving average — not a model. If a reviewer asks "walk me through your ML pipeline", there is no pipeline to walk
  through. You need to clearly separate: rule-based threshold alerts (now) vs. ML demand forecasting (v2 roadmap).

  9. No NDPA slide
  The panel said explicitly: "This is a lesson for everyone." If you don't mention it, you're the team that ignored the direct instruction.

  10. No competitor analysis table ready
  Not for the main deck — but if the Q&A goes there and you have nothing, you look unprepared. It should take 30 minutes to build.

  ---
  The Sequence to Fix Things

  Fix transfer approval flow (inverted logic)     ← USP depends on this
  Fix Procurement Officer warehouse scope         ← core role model is wrong
  Fix manager update UI bug                       ← demo confidence
  Add alert filtering by warehouse                ← multi-warehouse credibility
  Add movements cursor pagination                 ← reviewer asked, easy win
  Add PO audit trail                              ← reviewer asked, easy win

  Then prepare:
    - Expiry loss slide (roadmap, v2)
    - AI vs. heuristic clarity table
    - NDPA compliance slide
    - Competitor analysis table (for Q&A only)
    - Fallback screenshots + 3-min recorded demo

  The first two items are not cosmetic — they are structural bugs in your core feature. Fix those first and everything else is presentation polish.