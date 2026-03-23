# Tasks: NexusFin Equity Service Baseline

**Input**: Design documents from `/specs/001-equity-service-baseline/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tasks include the verification required by the constitution. Core business methods need unit tests, and API or callback changes need integration coverage before feature completion.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Include exact file paths in every task description

## Path Conventions

- Application code: `src/main/java/com/nexusfin/equity/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/com/nexusfin/equity/` and `src/test/resources/`
- Feature specs: `specs/001-equity-service-baseline/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare build, runtime configuration, and database bootstrap files needed by all stories.

- [X] T001 Update Maven build plugins and baseline dependencies for validation, H2, and checkstyle in `pom.xml`
- [X] T002 Configure datasource, MyBatis-Plus, logging, and partner properties in `src/main/resources/application.yml` and `src/test/resources/application.yml`
- [X] T003 [P] Add project checkstyle rules in `checkstyle.xml`
- [X] T004 [P] Add baseline MySQL and H2 schema scripts in `src/main/resources/db/schema.sql` and `src/test/resources/db/schema-h2.sql`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared domain, persistence, security, idempotency, and exception infrastructure that all stories depend on.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T005 Create shared business enums in `src/main/java/com/nexusfin/equity/enums/MemberStatusEnum.java`, `src/main/java/com/nexusfin/equity/enums/BenefitOrderStatusEnum.java`, `src/main/java/com/nexusfin/equity/enums/PaymentTypeEnum.java`, `src/main/java/com/nexusfin/equity/enums/PaymentStatusEnum.java`, `src/main/java/com/nexusfin/equity/enums/SignStatusEnum.java`, `src/main/java/com/nexusfin/equity/enums/NotificationTypeEnum.java`, and `src/main/java/com/nexusfin/equity/enums/NotificationProcessStatusEnum.java`
- [X] T006 [P] Create core aggregate entities in `src/main/java/com/nexusfin/equity/entity/MemberInfo.java`, `src/main/java/com/nexusfin/equity/entity/MemberChannel.java`, `src/main/java/com/nexusfin/equity/entity/BenefitProduct.java`, `src/main/java/com/nexusfin/equity/entity/BenefitOrder.java`, `src/main/java/com/nexusfin/equity/entity/PaymentRecord.java`, `src/main/java/com/nexusfin/equity/entity/SignTask.java`, `src/main/java/com/nexusfin/equity/entity/ContractArchive.java`, `src/main/java/com/nexusfin/equity/entity/NotificationReceiveLog.java`, and `src/main/java/com/nexusfin/equity/entity/IdempotencyRecord.java`
- [X] T007 [P] Create shared repository interfaces in `src/main/java/com/nexusfin/equity/repository/MemberInfoRepository.java`, `src/main/java/com/nexusfin/equity/repository/MemberChannelRepository.java`, `src/main/java/com/nexusfin/equity/repository/BenefitProductRepository.java`, `src/main/java/com/nexusfin/equity/repository/BenefitOrderRepository.java`, `src/main/java/com/nexusfin/equity/repository/PaymentRecordRepository.java`, `src/main/java/com/nexusfin/equity/repository/SignTaskRepository.java`, `src/main/java/com/nexusfin/equity/repository/ContractArchiveRepository.java`, `src/main/java/com/nexusfin/equity/repository/NotificationReceiveLogRepository.java`, and `src/main/java/com/nexusfin/equity/repository/IdempotencyRecordRepository.java`
- [X] T008 [P] Implement signature, trace, and sensitive-data utilities in `src/main/java/com/nexusfin/equity/util/SignatureUtil.java`, `src/main/java/com/nexusfin/equity/util/TraceIdUtil.java`, `src/main/java/com/nexusfin/equity/util/SensitiveDataUtil.java`, and `src/main/java/com/nexusfin/equity/util/RequestIdUtil.java`
- [X] T009 Implement request signature verification and trace propagation in `src/main/java/com/nexusfin/equity/config/SignatureProperties.java`, `src/main/java/com/nexusfin/equity/config/TraceIdFilter.java`, and `src/main/java/com/nexusfin/equity/config/WebMvcConfig.java`
- [X] T010 Implement idempotency persistence and coordination in `src/main/java/com/nexusfin/equity/service/IdempotencyService.java`, `src/main/java/com/nexusfin/equity/service/impl/IdempotencyServiceImpl.java`, and `src/main/java/com/nexusfin/equity/entity/IdempotencyRecord.java`
- [X] T011 Implement legal order and payment transition guards in `src/main/java/com/nexusfin/equity/util/OrderStateMachine.java`
- [X] T012 Update standardized error and validation handling in `src/main/java/com/nexusfin/equity/exception/BizException.java`, `src/main/java/com/nexusfin/equity/exception/GlobalExceptionHandler.java`, and `src/main/java/com/nexusfin/equity/dto/response/Result.java`
- [X] T013 Add foundational unit tests for signature, idempotency, and state transitions in `src/test/java/com/nexusfin/equity/config/SignatureServiceTest.java`, `src/test/java/com/nexusfin/equity/service/IdempotencyServiceTest.java`, and `src/test/java/com/nexusfin/equity/util/OrderStateMachineTest.java`

**Checkpoint**: Foundation is ready. User stories can now be implemented with shared security, persistence, and state-transition rules in place.

---

## Phase 3: User Story 1 - 拒件用户承接与权益下单 (Priority: P1) 🎯 MVP

**Goal**: Idempotently onboard diverted rejected users, show purchasable benefit products, and create traceable benefit orders.

**Independent Test**: Push the same diverted user twice, query the product page, then create one benefit order and confirm only one `member_id`, one channel binding, and one `benefit_order_no` are produced.

### Tests for User Story 1

- [X] T014 [P] [US1] Add unit tests for silent registration and order creation in `src/test/java/com/nexusfin/equity/service/MemberOnboardingServiceTest.java` and `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`
- [X] T015 [P] [US1] Add API integration tests for `/api/users/register`, `/api/equity/products/{productCode}`, `/api/equity/orders`, and `/api/equity/orders/{benefitOrderNo}` in `src/test/java/com/nexusfin/equity/controller/UserRegistrationControllerIntegrationTest.java` and `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`

### Implementation for User Story 1

- [X] T016 [P] [US1] Create registration and order DTOs in `src/main/java/com/nexusfin/equity/dto/request/RegisterUserRequest.java`, `src/main/java/com/nexusfin/equity/dto/request/CreateBenefitOrderRequest.java`, `src/main/java/com/nexusfin/equity/dto/response/RegisterUserResponse.java`, `src/main/java/com/nexusfin/equity/dto/response/ProductPageResponse.java`, `src/main/java/com/nexusfin/equity/dto/response/CreateBenefitOrderResponse.java`, and `src/main/java/com/nexusfin/equity/dto/response/BenefitOrderStatusResponse.java`
- [X] T017 [P] [US1] Implement member and order persistence queries in `src/main/java/com/nexusfin/equity/repository/MemberInfoRepository.java`, `src/main/java/com/nexusfin/equity/repository/MemberChannelRepository.java`, `src/main/java/com/nexusfin/equity/repository/BenefitProductRepository.java`, and `src/main/java/com/nexusfin/equity/repository/BenefitOrderRepository.java`
- [X] T018 [US1] Implement idempotent silent registration and channel binding in `src/main/java/com/nexusfin/equity/service/MemberOnboardingService.java` and `src/main/java/com/nexusfin/equity/service/impl/MemberOnboardingServiceImpl.java`
- [X] T019 [US1] Implement product display, benefit order creation, and order status query in `src/main/java/com/nexusfin/equity/service/BenefitOrderService.java` and `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- [X] T020 [US1] Implement signed registration entrypoint in `src/main/java/com/nexusfin/equity/controller/UserRegistrationController.java`
- [X] T021 [US1] Implement product, order creation, and order status endpoints with `traceId` and `bizOrderNo` logging in `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java`

**Checkpoint**: User Story 1 is complete when onboarding and order creation are idempotent and the created order can be queried independently.

---

## Phase 4: User Story 2 - 权益签约与首次代扣 (Priority: P2)

**Goal**: Track agreement signing, persist contract evidence, process first-deduct results, and distinguish direct-continuation versus fallback-eligible orders.

**Independent Test**: Seed an existing benefit order, complete the sign flow, process one successful and one failed first-deduct callback, and confirm order state, payment record, and downstream routing fields are updated correctly.

### Tests for User Story 2

- [X] T022 [P] [US2] Add unit tests for sign-task lifecycle and first-deduct routing in `src/test/java/com/nexusfin/equity/service/AgreementServiceTest.java` and `src/test/java/com/nexusfin/equity/service/PaymentServiceTest.java`
- [X] T023 [P] [US2] Add integration tests for first-deduct callback handling in `src/test/java/com/nexusfin/equity/controller/PaymentCallbackControllerIntegrationTest.java`

### Implementation for User Story 2

- [X] T024 [P] [US2] Create signing and payment DTOs in `src/main/java/com/nexusfin/equity/dto/request/DeductionCallbackRequest.java`, `src/main/java/com/nexusfin/equity/dto/response/SignTaskResponse.java`, and `src/main/java/com/nexusfin/equity/dto/response/PaymentStatusResponse.java`
- [X] T025 [P] [US2] Implement signing and payment persistence queries in `src/main/java/com/nexusfin/equity/repository/SignTaskRepository.java`, `src/main/java/com/nexusfin/equity/repository/ContractArchiveRepository.java`, and `src/main/java/com/nexusfin/equity/repository/PaymentRecordRepository.java`
- [X] T026 [US2] Implement sign-task generation, contract archive persistence, and sign-state validation in `src/main/java/com/nexusfin/equity/service/AgreementService.java` and `src/main/java/com/nexusfin/equity/service/impl/AgreementServiceImpl.java`
- [X] T027 [US2] Implement first-deduct initiation, callback persistence, and path A/path B transition logic in `src/main/java/com/nexusfin/equity/service/PaymentService.java` and `src/main/java/com/nexusfin/equity/service/impl/PaymentServiceImpl.java`
- [X] T028 [US2] Implement first-deduction callback endpoint with signed-request validation in `src/main/java/com/nexusfin/equity/controller/PaymentCallbackController.java`
- [X] T029 [US2] Implement downstream YunKa push for first-deduct-success and fallback-pending orders in `src/main/java/com/nexusfin/equity/service/DownstreamSyncService.java` and `src/main/java/com/nexusfin/equity/service/impl/DownstreamSyncServiceImpl.java`

**Checkpoint**: User Story 2 is complete when one order can complete signing and first deduct, and both success and failure paths remain queryable and auditable.

---

## Phase 5: User Story 3 - 放款结果消费、兜底代扣与状态闭环 (Priority: P3)

**Goal**: Consume downstream grant and repayment notifications, trigger exactly one fallback deduct when eligible, and close the loop for exercise and refund outcomes.

**Independent Test**: Seed a fallback-eligible order, replay grant-success, repayment, exercise, and refund callbacks, and confirm the service triggers at most one fallback deduct while preserving full notification history.

### Tests for User Story 3

- [X] T030 [P] [US3] Add unit tests for fallback deduct orchestration and notification idempotency in `src/test/java/com/nexusfin/equity/service/FallbackDeductServiceTest.java` and `src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java`
- [X] T031 [P] [US3] Add integration tests for grant, repayment, exercise, refund, and fallback callbacks in `src/test/java/com/nexusfin/equity/controller/NotificationCallbackControllerIntegrationTest.java` and `src/test/java/com/nexusfin/equity/controller/MySqlCallbackFlowIntegrationTest.java`

### Implementation for User Story 3

- [X] T032 [P] [US3] Create callback and exercise DTOs in `src/main/java/com/nexusfin/equity/dto/request/ExerciseCallbackRequest.java`, `src/main/java/com/nexusfin/equity/dto/request/RefundCallbackRequest.java`, `src/main/java/com/nexusfin/equity/dto/request/GrantForwardCallbackRequest.java`, `src/main/java/com/nexusfin/equity/dto/request/RepaymentForwardCallbackRequest.java`, and `src/main/java/com/nexusfin/equity/dto/response/ExerciseUrlResponse.java`
- [X] T033 [P] [US3] Implement notification query support in `src/main/java/com/nexusfin/equity/repository/NotificationReceiveLogRepository.java` and `src/main/java/com/nexusfin/equity/repository/BenefitOrderRepository.java`
- [X] T034 [US3] Implement grant consumption, repayment tracking, exercise/refund updates, and notification audit persistence in `src/main/java/com/nexusfin/equity/service/NotificationService.java` and `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java`
- [X] T035 [US3] Implement one-time fallback deduct orchestration after grant success in `src/main/java/com/nexusfin/equity/service/FallbackDeductService.java` and `src/main/java/com/nexusfin/equity/service/impl/FallbackDeductServiceImpl.java`
- [X] T036 [US3] Extend callback endpoints for fallback deduction, grant, repayment, exercise, and refund processing in `src/main/java/com/nexusfin/equity/controller/PaymentCallbackController.java` and `src/main/java/com/nexusfin/equity/controller/NotificationCallbackController.java`
- [X] T037 [US3] Expose granted-order exercise URL query in `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java` and `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`

**Checkpoint**: User Story 3 is complete when callback replay is safe, fallback deduct is triggered at most once per eligible grant-success event, and closed-loop state remains queryable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish reconciliation, smoke validation, and delivery documentation across all stories.

- [X] T038 [P] Add reconciliation query support by core identifiers in `src/main/java/com/nexusfin/equity/service/ReconciliationService.java`, `src/main/java/com/nexusfin/equity/service/impl/ReconciliationServiceImpl.java`, and `src/main/java/com/nexusfin/equity/repository/NotificationReceiveLogRepository.java`
- [X] T039 Add end-to-end quickstart smoke coverage in `src/test/java/com/nexusfin/equity/NexusfinEquityApplicationTests.java` and `src/test/resources/db/test-data.sql`
- [X] T040 Update delivery and verification documentation in `specs/001-equity-service-baseline/quickstart.md`, `AGENTS.md`, and `README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no prerequisites and can start immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all story work.
- **Phase 3: US1** depends on Phase 2.
- **Phase 4: US2** depends on Phase 2 and uses seeded orders for independent validation, but is safest to execute after US1 establishes the order aggregate.
- **Phase 5: US3** depends on Phase 2 and uses seeded eligible orders for independent validation, but is safest to execute after US2 establishes payment and routing behavior.
- **Phase 6: Polish** depends on the stories you intend to ship.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories after foundational work.
- **US2 (P2)**: Business flow builds on the benefit order aggregate; tests can seed prerequisite order data if implemented separately.
- **US3 (P3)**: Business flow builds on payment and grant state; tests can seed prerequisite order and payment data if implemented separately.

### Within Each User Story

- Write tests first and verify they fail before implementation.
- Complete DTO and repository work before service logic.
- Complete service logic before controller or callback entrypoints.
- Keep logging, idempotency, and audit writes in the same story phase as the state transition they protect.

### Parallel Opportunities

- `T003` and `T004` can run in parallel after `T001`.
- `T006`, `T007`, and `T008` can run in parallel after `T005`.
- `T014` and `T015` can run in parallel for US1.
- `T016` and `T017` can run in parallel for US1.
- `T022` and `T023` can run in parallel for US2.
- `T024` and `T025` can run in parallel for US2.
- `T030` and `T031` can run in parallel for US3.
- `T032` and `T033` can run in parallel for US3.
- `T038` can run in parallel with final documentation cleanup once all stories are stable.

---

## Parallel Example: User Story 1

```bash
Task T014: Add unit tests in src/test/java/com/nexusfin/equity/service/MemberOnboardingServiceTest.java
Task T015: Add API integration tests in src/test/java/com/nexusfin/equity/controller/UserRegistrationControllerIntegrationTest.java

Task T016: Create DTOs in src/main/java/com/nexusfin/equity/dto/request/RegisterUserRequest.java
Task T017: Implement repositories in src/main/java/com/nexusfin/equity/repository/MemberInfoRepository.java
```

## Parallel Example: User Story 2

```bash
Task T022: Add unit tests in src/test/java/com/nexusfin/equity/service/AgreementServiceTest.java
Task T023: Add callback integration test in src/test/java/com/nexusfin/equity/controller/PaymentCallbackControllerIntegrationTest.java

Task T024: Create DTOs in src/main/java/com/nexusfin/equity/dto/request/DeductionCallbackRequest.java
Task T025: Implement repositories in src/main/java/com/nexusfin/equity/repository/PaymentRecordRepository.java
```

## Parallel Example: User Story 3

```bash
Task T030: Add unit tests in src/test/java/com/nexusfin/equity/service/FallbackDeductServiceTest.java
Task T031: Add integration tests in src/test/java/com/nexusfin/equity/controller/NotificationCallbackControllerIntegrationTest.java

Task T032: Create DTOs in src/main/java/com/nexusfin/equity/dto/request/GrantForwardCallbackRequest.java
Task T033: Implement notification queries in src/main/java/com/nexusfin/equity/repository/NotificationReceiveLogRepository.java
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Run `mvn test`, `mvn clean package -DskipTests`, and `mvn checkstyle:check`.
5. Validate duplicate registration replay and order creation before moving on.

### Incremental Delivery

1. Deliver US1 for stable onboarding and order creation.
2. Deliver US2 for signing, first deduct, and downstream routing.
3. Deliver US3 for grant consumption, fallback deduct, and callback closure.
4. Finish with reconciliation and operational documentation.

### Suggested MVP Scope

- Ship through **User Story 1** first.
- Keep **User Story 2** behind the same order aggregate after US1 is stable.
- Add **User Story 3** only after callback idempotency and payment routing are proven in lower environments.
