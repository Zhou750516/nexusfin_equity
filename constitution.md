# NexusFin Equity Constitution

## Source Basis

This constitution is derived from the project documents under `docs/`:

- `惠聚项目-技术概要设计方案.pdf`
- `NexusFin 接口文档.pdf`
- `NexusFin 接口+数据表梳理.pdf`
- `NexusFin 数据库ER关系说明.pdf`
- `Nexusfin-equity 目录结构.pdf`

It serves as the repository-level summary of the non-negotiable business,
interface, data, and implementation constraints defined by those materials.

## Core Principles

### 1. Cross-System Boundary Ownership

`nexusfin-equity` is the ABS-side equity distribution service. It is responsible
for user silent registration, benefit order lifecycle management, contract
coordination, payment/withhold handling, and consuming downstream status
notifications. YunKa remains the transit hub between ABS and KJ. New work MUST
respect this boundary model and MUST NOT bypass the documented cross-system flow.

### 2. Secure, Signed, Idempotent APIs

All external interfaces MUST use HTTPS and `application/json`. Responses MUST
follow the unified structure `code + message + data`. State-changing requests
MUST support idempotency through a unique `request_id`. System calls MUST carry
the signed headers `X-App-Id`, `X-Timestamp`, `X-Nonce`, and `X-Signature`, and
signature generation MUST remain based on HMAC-SHA256 plus AppSecret. IP
allowlisting remains part of the first protection layer.

### 3. Privacy-First Domain Data

ABS and YK databases MUST stay physically isolated and associate through logical
keys such as `benefit_order_no`, `order_no`, `member_id`, and external IDs.
Sensitive fields such as mobile number, real name, and identity number MUST be
encrypted at rest. Query-critical sensitive identifiers MUST support secure
matching strategies. Order, payment, contract, and notification data MUST use
explicit state machines rather than informal free-text transitions.

### 4. Order-Centric Traceability

The service MUST preserve end-to-end traceability across registration, benefit
purchase, first deduct, fallback deduct, exercise, refund, grant callback, and
repayment callback flows. Critical records MUST be traceable through durable
keys such as `request_id`, `benefit_order_no`, `payment_no`, `task_no`, and
`notify_no`. Callback processing and notification forwarding MUST leave auditable
logs and retry-aware status records.

### 5. Reliability, Performance, and Observability

Core transaction paths MUST be designed toward the documented targets: P99
response time under 500ms, peak TPS above 500, and core link availability at or
above 99.95%. Monitoring, alerting, structured logs, and business-state
visibility are mandatory design requirements. Any feature that weakens these
targets requires an explicit mitigation and review decision.

## Implementation Baseline

For this repository, the active implementation stack is Java 17, Spring Boot
3.2.x, Maven, MyBatis-Plus, and MySQL 8.0. Package layout follows
`docs/Nexusfin-equity 目录结构.pdf` and the current codebase:

- `controller/`
- `service/`
- `service/impl/`
- `repository/`
- `entity/`
- `dto/request/`
- `dto/response/`
- `config/`
- `exception/`
- `enums/`
- `util/`

## Change Governance

Any feature that changes interfaces, table design, order states, or callback
behavior MUST update the affected docs in `docs/` together with the code change.
If business contracts and repository guidance conflict, this constitution and
the current `docs/` set take precedence.

Version: `1.1.0`  
Ratified: `2026-03-23`  
Last Amended: `2026-03-23`
