# Implementation Plan: NexusFin Equity Service Baseline

**Branch**: `001-equity-service-baseline` | **Date**: 2026-03-23 | **Spec**: [spec.md](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/specs/001-equity-service-baseline/spec.md)
**Input**: Feature specification from `/specs/001-equity-service-baseline/spec.md`

## Summary

Build the baseline ABS-side `nexusfin-equity` service around five stable
capabilities: rejected-user silent registration, benefit product and order
management, agreement signing, first/fallback deduct handling, and downstream
grant/repayment notification consumption. The implementation will align the
current repository structure with the business contracts and ER model defined in
the architecture and interface documents, while keeping cross-system boundaries,
idempotency, security signing, and auditability explicit.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.x, Spring Validation, MyBatis-Plus,
MySQL Connector, Lombok  
**Storage**: MySQL 8.0 (`nexusfin_equity`), optional Redis/Redisson for
idempotency and distributed locking in critical flows  
**Testing**: JUnit 5, Spring Boot Test, H2 for isolated tests, Maven Surefire  
**Target Platform**: Linux container or VM deployment for backend service,
supporting ABS H5 clients and signed server-to-server integrations  
**Project Type**: Single Spring Boot web service  
**Performance Goals**: P99 core transaction RT < 500ms, peak TPS > 500 on core
paths, core SLA >= 99.95%  
**Constraints**: HTTPS + signed headers, request-level idempotency, encrypted
sensitive fields, no duplicate payment or callback effects, audit retention for
critical financial logs, repository-layer DB access only  
**Scale/Scope**: Initial design point of ~10,000 diverted users/day, ~500 daily
loan conversions, ABS service deployed as 2 active instances in production

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- `Result<T>` response envelope, `@Valid` request validation, and versionless API
  paths are identified for all ABS-owned HTTP contracts. Pass.
- Business logic placement remains `controller -> service -> repository`, and the
  design avoids database access inside loops by using aggregate loads, idempotent
  keys, and state-based updates. Pass.
- Domain data rules are explicit: snake_case DB mapping, camelCase Java fields,
  encrypted personal information, logical cross-system linkage through
  `benefit_order_no`, and stable order/payment/signing state machines. Pass.
- Operational rules are covered: signed request verification, callback
  traceability, SLF4J logging with `traceId` + `bizOrderNo`, and exception
  handling through the global pipeline. Pass.
- Verification plan includes `mvn test`, `mvn clean package -DskipTests`, and
  `mvn checkstyle:check`, with unit and callback-flow tests required before
  feature completion. Pass.

## Project Structure

### Documentation (this feature)

```text
specs/001-equity-service-baseline/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── abs-public-api.yaml
│   └── abs-callback-api.yaml
└── tasks.md
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/nexusfin/equity/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── service/impl/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/request/
│   │   ├── dto/response/
│   │   ├── config/
│   │   ├── exception/
│   │   ├── enums/
│   │   └── util/
│   └── resources/
└── test/
    ├── java/com/nexusfin/equity/
    └── resources/
```

**Structure Decision**: Keep a single Spring Boot backend and express the ABS
baseline as a cohesive service. Public controllers will expose ABS-owned H5 and
callback interfaces, service implementations will coordinate state transitions,
and repository interfaces will isolate persistence concerns around the ER model.

## Post-Design Constitution Check

- Cross-system ownership remains intact: ABS receives KJ or YK input, delegates
  transit responsibilities to YK, and does not absorb KJ/YK-specific orchestration
  logic. Pass.
- Signed and idempotent API rules are reflected in the contract artifacts for
  both public and callback endpoints. Pass.
- Privacy-first data rules are embedded in the data model via encrypted member
  fields and logical linkage keys. Pass.
- Order-centric traceability is supported through `request_id`, `member_id`,
  `benefit_order_no`, `payment_no`, `task_no`, and notification log identifiers.
  Pass.
- Reliability and observability are covered in quickstart verification and
  research decisions, including retry-safe callback handling and audit logging.
  Pass.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
