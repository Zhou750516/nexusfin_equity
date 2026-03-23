# Research: NexusFin Equity Service Baseline

## Decision 1: Use the repository's active Java 17 + Spring Boot 3.2.x stack

**Decision**: Plan the baseline service on top of Java 17, Spring Boot 3.2.x,
MyBatis-Plus, Maven, and MySQL 8.0.

**Rationale**: The repository is already scaffolded on this stack, and the local
constitution explicitly sets it as the active implementation baseline. The
technical overview document describes a Java + Spring ecosystem and MySQL +
MyBatis-Plus data layer, so keeping the repo-aligned version avoids planning for
an architecture that is already outdated relative to the codebase.

**Alternatives considered**:

- Spring Boot 2.7.x exactly as shown in the architecture PDF: rejected because
  the repository has already moved to 3.2.x and planning against 2.7.x would
  create immediate divergence.
- A lighter HTTP stack without Spring: rejected because it would conflict with
  the established repository and team setup.

## Decision 2: Treat ABS as the owner of order and payment state, not transit flow

**Decision**: The baseline implementation will keep ABS responsible for member
registration, benefit orders, agreements, payment records, and consumption of
callbacks, while YunKa remains the transit hub for trial, grant, and repayment
forwarding.

**Rationale**: Both the technical overview and interface docs emphasize the
bridge model. ABS should not absorb YunKa's order-transit duties because that
would re-couple KJ and ABS and contradict the architecture's decoupling goal.

**Alternatives considered**:

- Allow ABS to directly own trial/grant orchestration: rejected because it
  bypasses the documented YunKa transit layer.
- Merge ABS and YK order models in one service: rejected because the documents
  require physical database isolation and independent system evolution.

## Decision 3: Model all critical state changes as explicit state machines

**Decision**: Represent benefit-order lifecycle, sign-task lifecycle,
first/fallback deduct lifecycle, and notification processing lifecycle through
explicit statuses with controlled transitions.

**Rationale**: The ER note and risk-control section both require illegal state
jumps to be rejected and traced. State-machine modeling is necessary to protect
against duplicate callbacks, retry races, and financial inconsistencies.

**Alternatives considered**:

- Free-form text states or implicit boolean flags: rejected because they do not
  support safe transition validation.
- Reconstructing state solely from external callbacks: rejected because ABS must
  remain auditable even when external systems are unavailable.

## Decision 4: Use request-level idempotency plus order-scoped locking for critical writes

**Decision**: Protect state-changing inbound and outbound operations through
`request_id`-based idempotency and order-scoped coordination for high-risk
financial operations such as payment callback handling and fallback deduct
triggering.

**Rationale**: The documents explicitly require idempotency for POST requests
and recommend distributed locking for payment and grant-sensitive operations.
The combination prevents duplicate effects during retry, timeout, and callback
reordering scenarios.

**Alternatives considered**:

- Rely only on database unique constraints: rejected because business operations
  such as fallback triggering need semantic deduplication, not only row-level
  uniqueness.
- Rely only on in-memory synchronization: rejected because production deployment
  is multi-instance.

## Decision 5: Keep cross-system contracts JSON over HTTPS with signed headers

**Decision**: Define ABS public and callback contracts as JSON/HTTPS interfaces
with the shared signed header scheme and unified response envelope.

**Rationale**: This is the common interface rule across all provided design
documents. Planning on that basis ensures compatibility with KJ, YK, and the
benefit supplier integration model.

**Alternatives considered**:

- Internal-only contracts without the shared envelope: rejected because they
  would diverge from the cross-party agreement.
- RPC-specific contracts: rejected because the docs normalize cross-system
  integration on HTTP REST style.

## Decision 6: Use `benefit_order_no` as the primary cross-system business key

**Decision**: Anchor ABS-to-YK correlation and audit trails on
`benefit_order_no`, while also preserving `member_id`, `payment_no`, `task_no`,
`loan_order_no`, and callback request IDs.

**Rationale**: The ER document identifies `benefit_order_no` as the core
cross-database linkage. Without it, downstream reconciliation and traceability
break down across the isolated ABS and YK databases.

**Alternatives considered**:

- Correlate only via KJ user identifiers: rejected because one user can have
  multiple orders and order-level traceability would be lost.
- Correlate only via YunKa transit order: rejected because ABS is the generator
  of the originating business order.

## Decision 7: Plan observability around structured logs and audit-grade records

**Decision**: Treat structured logging, callback processing logs, payment logs,
and order-audit records as baseline artifacts, not optional enhancements.

**Rationale**: The architecture document requires structured JSON logs, trace
IDs, T+1 reconciliation, and long-lived audit retention for financial events.
Planning without those artifacts would fail the operational objectives.

**Alternatives considered**:

- Rely only on application logs without dedicated receive/notify records:
  rejected because business reconciliation needs queryable durable records.
- Add observability later: rejected because the docs treat it as a first-class
  requirement.
