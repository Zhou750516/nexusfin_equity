<!--
Sync Impact Report
Version change: 1.0.0 -> 1.1.0
Modified principles:
- I. API Contract Discipline -> I. Cross-System Boundary Ownership
- II. Layered Service Boundaries -> II. Secure, Signed, Idempotent APIs
- III. Domain Data Integrity -> III. Privacy-First Domain Data
- IV. Operational Traceability -> IV. Order-Centric Traceability
- V. Verifiable Quality Gates -> V. Reliability, Performance, and Observability
Added sections:
- Source Document Baseline
Removed sections:
- None
Templates requiring updates:
- ⚠ pending review: .specify/templates/plan-template.md
- ⚠ pending review: .specify/templates/spec-template.md
- ⚠ pending review: .specify/templates/tasks-template.md
- ⚠ pending review: AGENTS.md
Follow-up TODOs:
- None
-->
# NexusFin Equity Constitution

## Core Principles

### I. Cross-System Boundary Ownership
`nexusfin-equity` is the ABS-side equity distribution service in the NexusFin
closed loop. It MUST only perform ABS responsibilities: static user acceptance,
benefit order management, contract orchestration, payment/withhold handling,
and downstream state consumption from YunKa. Cross-party interaction MUST
follow the designed bridge model where YunKa acts as the transit hub and KJ,
YK, ABS evolve independently through documented interfaces. New features MUST
preserve this decoupling and MUST NOT introduce undocumented direct
cross-system shortcuts.

### II. Secure, Signed, Idempotent APIs
All inter-system APIs MUST use HTTPS, `application/json`, and the unified
response envelope containing `code`, `message`, and `data`. Requests that create
resources or change state MUST carry a unique `request_id` for idempotency.
System-to-system calls MUST include the signed headers `X-App-Id`,
`X-Timestamp`, `X-Nonce`, and `X-Signature`, with signatures generated through
HMAC-SHA256 using AppSecret, and gateway IP allowlists MUST remain part of the
first security boundary. Any interface change is incomplete until these contract
rules remain explicit and testable.

### III. Privacy-First Domain Data
ABS and YK databases MUST stay physically isolated, with business linkage built
through logical identifiers such as `benefit_order_no`, `order_no`,
`member_id`, and external user IDs. Sensitive data including mobile number,
identity number, and real name MUST be encrypted at rest, and lookup-oriented
fields MUST support secure indexed matching where business queries require it.
Order, payment, contract, and notification records MUST model explicit status
machines rather than ad hoc free-text states. Internal monetary handling MUST be
consistent and documented at every service boundary.

### IV. Order-Centric Traceability
The system MUST be able to reconstruct a full order journey across silent
registration, benefit purchase, first deduct, fallback deduct, exercise,
refund, grant result, and repayment result flows. Every critical interaction
MUST be traceable through durable identifiers such as `request_id`,
`benefit_order_no`, `payment_no`, `task_no`, `notify_no`, and partner order
numbers. Callback handling, notification forwarding, and retry behavior MUST
leave auditable logs and state transitions so that reconciliation, issue triage,
and dispute handling can be completed from stored records alone.

### V. Reliability, Performance, and Observability
Core transaction interfaces such as order creation, payment callback handling,
and downstream notification processing MUST be designed toward the program-level
targets described in the architecture documents: P99 response time under 500ms
for core chains, peak TPS above 500, core link availability at or above 99.95%,
and no single point on critical runtime paths. Monitoring, alerting, structured
logging, and business-state observability are mandatory design elements rather
than optional implementation polish. Any feature that weakens these properties
requires an explicit mitigation plan before implementation.

## Source Document Baseline

This constitution is derived from the repository source documents under `docs/`,
especially the technical overview, interface specification, interface plus data
table summary, ER relationship note, and package layout note. Those documents
establish the business flow of reject diversion, equity first then pay later,
YunKa transit orchestration, and the database responsibilities of ABS and YK.

For this repository's implementation baseline, Java 17, Spring Boot 3.2.x,
Maven, MyBatis-Plus, and MySQL 8.0 are the active service stack. Package layout
for `nexusfin-equity` MUST follow the current repository structure under
`src/main/java/com/nexusfin/equity/`, and interface or table changes MUST keep
the corresponding `docs/` artifacts synchronized in the same change when the
business contract is affected.

## Delivery Workflow & Review Gates

Any feature that changes interfaces, order states, payment flow, contract flow,
or table structures MUST first identify the impacted document set in `docs/`.
Implementation is not complete until code, API behavior, and documentation agree
on the same field definitions, state transitions, and ownership boundaries.
Reviewers MUST treat cross-system compatibility, callback idempotency,
reconciliation traceability, and security signature correctness as merge gates.

When a change adds or redefines a business state, callback, or persistence
relationship, the developer MUST update the relevant DTOs, entities, repository
logic, and doc artifacts together. If a temporary deviation is unavoidable, the
plan and review notes MUST name the gap, the risk window, and the remediation
owner explicitly.

## Governance

This constitution supersedes stale local notes when they conflict with the
documented NexusFin business architecture and current repository implementation.
Amendments MUST be committed in a reviewable change that updates this file and
any affected docs, templates, or runtime guidance in the same change. Versioning
uses semantic rules: MAJOR for incompatible governance changes, MINOR for added
principles or materially expanded obligations, and PATCH for clarifications that
do not change intent.

Constitution compliance MUST be checked during planning, implementation, and
review. Where multiple sources disagree, the precedence order is: this
constitution, then current `docs/` business documents, then repository-local
guidance files.

**Version**: 1.1.0 | **Ratified**: 2026-03-23 | **Last Amended**: 2026-03-23
