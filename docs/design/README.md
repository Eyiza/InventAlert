# Design Documents & Diagrams

## Architecture Diagrams

All diagrams are in the [`Design Diagrams/`](../../Design%20Diagrams/) folder at the project root.

| Diagram | File | Description |
|---|---|---|
| High-Level Architecture | [High Level Architecture.png](../../Design%20Diagrams/High%20Level%20Architecture.png) | Full system overview: services, databases, Kafka, Nginx, frontend |
| Entity Relationship Diagram | [ERD.png](../../Design%20Diagrams/ERD.png) | All entities and their relationships |
| Class Diagram | [Class Diagram.png](../../Design%20Diagrams/Class%20Diagram.png) | UML class model for the domain |
| Use Case Diagram | [Use Case.png](../../Design%20Diagrams/Use%20Case.png) | User interactions by role |

## Activity Diagrams

| Flow | File |
|---|---|
| Company Onboarding | [Activity - Company Onboarding.png](../../Design%20Diagrams/Activity%20-%20Company%20Onboarding.png) |
| Sale / Outbound Movement | [Activity - Sale.png](../../Design%20Diagrams/Activity%20-%20Sale.png) |
| Transfer Suggestion | [Activity - Transfer Suggestion.png](../../Design%20Diagrams/Activity%20-%20Transfer%20Suggestion.png) |
| Reconciliation | [Activity - Reconciliation.png](../../Design%20Diagrams/Activity%20-%20Reconciliation.png) |
| Password Recovery | [Activity - Password Reconciliation.png](../../Design%20Diagrams/Activity%20-%20Password%20Reconciliation.png) |

## Entity-Relationship Diagram (Mermaid source)

The Mermaid source for the ERD is at [`erd.md`](../../erd.md) at the project root. Render it in any Mermaid-compatible viewer (GitHub, VS Code with Mermaid plugin, etc.).

## Product Requirements Document

The full PRD (54 KB) is at [`prd.md`](../../prd.md). It covers:
- Business context and problem statement
- User personas and role definitions
- Feature specifications for all five user roles
- Non-functional requirements (security, performance, multi-tenancy)
- Acceptance criteria per feature
