# Tasks: 惠选卡权益与借款流程 V1 页面交互理解

**Input**: Design documents from `/specs/004-benefit-loan-flow/` and latest alignment docs under `/docs/plan/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tasks include verification needed by the constitution. Core business methods require unit tests, and API or persistence changes add controller or integration coverage where needed.

**Organization**: Tasks are grouped by delivery phase so work can be executed against the current code baseline rather than the original “from scratch” assumption.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story or shared phase this belongs to (e.g. `US1`, `US2`, `US3`, `SHARED`)
- Include exact file paths in descriptions

## Phase 1: Historical Baseline (Already Completed)

**Purpose**: Preserve the record of early design and first-version implementation work that has already landed in the repository.

- [X] T001 Update `specs/004-benefit-loan-flow/plan.md` with initial technical context and constitution gates
- [X] T002 Create `specs/004-benefit-loan-flow/research.md`
- [X] T003 [P] Create `specs/004-benefit-loan-flow/data-model.md`
- [X] T004 [P] Create `specs/004-benefit-loan-flow/contracts/page-flow-api.md` and `specs/004-benefit-loan-flow/contracts/tech-platform-outbound.md`
- [X] T005 Create `specs/004-benefit-loan-flow/quickstart.md`
- [X] T006-T012 Foundation and first-version third-party client work already landed historically; keep as completed record

---

## Phase 2: Shared Alignment Foundation (Blocking)

**Purpose**: Align the current codebase to the latest “艾博生 -> 云卡 -> 小花” boundary and provide a reusable typed gateway layer for all later story work.

**⚠️ CRITICAL**: No story-specific alignment work should proceed until this phase is complete.

### Tests for Shared Foundation

- [ ] T013 [P] [SHARED] Add gateway service coverage in `src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java`
- [ ] T014 [P] [SHARED] Extend config binding coverage for Yunka paths in `src/test/java/com/nexusfin/equity/config/YunkaPropertiesTest.java` or the closest existing config test file

### Implementation for Shared Foundation

- [ ] T015 [SHARED] Extend `src/main/java/com/nexusfin/equity/config/YunkaProperties.java`, `src/main/resources/application.yml`, and `src/test/resources/application.yml` with paths for `protocol/queryProtocolAggregationLink`, `user/token`, `user/query`, `loan/repayPlan`, `card/smsSend`, `card/smsConfirm`, `card/userCards`, `credit/image/query`, and benefit sync
- [ ] T016 [P] [SHARED] Create typed Xiaohua-via-Yunka request and response models under `src/main/java/com/nexusfin/equity/thirdparty/yunka/` for protocol query, user token, user query, card list, loan repay plan, sms send/confirm, image query, and benefit sync
- [ ] T017 [SHARED] Create `src/main/java/com/nexusfin/equity/service/XiaohuaGatewayService.java` and `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java` to encapsulate path selection, request dispatch, response parsing, and upstream error mapping
- [ ] T018 [P] [SHARED] Create callback DTOs for latest loan and repayment callbacks under `src/main/java/com/nexusfin/equity/dto/request/` and align them with current Yunka-forwarded semantics
- [ ] T019 [SHARED] Update `src/main/java/com/nexusfin/equity/controller/NotificationCallbackController.java` and `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java` to consume the new callback DTOs while preserving idempotency and `traceId + bizOrderNo` logging requirements

**Checkpoint**: Shared Xiaohua-via-Yunka contract layer is ready and later stories can reuse it.

---

## Phase 3: User Story 1 - 在权益开通前展示动态协议与绑卡信息 (Priority: P1) 🎯 MVP

**Goal**: Make the benefits page reflect Xiaohua 4.22 dynamic protocol and user card data instead of relying only on local static composition.

**Independent Test**: Calling benefits-page endpoints returns locally configured display data plus upstream protocol/card details, and activation is blocked when required agreement conditions are not met.

### Tests for User Story 1

- [ ] T020 [P] [US1] Add service coverage for benefits-page aggregation in `src/test/java/com/nexusfin/equity/service/BenefitsServiceTest.java`
- [ ] T021 [P] [US1] Add controller integration coverage in `src/test/java/com/nexusfin/equity/controller/BenefitsControllerIntegrationTest.java`

### Implementation for User Story 1

- [ ] T022 [US1] Refactor `src/main/java/com/nexusfin/equity/service/impl/BenefitsServiceImpl.java` to query protocols through `XiaohuaGatewayService` and merge them into `GET /api/benefits/card-detail`
- [ ] T023 [US1] Refactor `src/main/java/com/nexusfin/equity/service/impl/BenefitsServiceImpl.java` and related response DTOs under `src/main/java/com/nexusfin/equity/dto/response/` to include dynamic user card summary from `card/userCards`
- [ ] T024 [US1] Update `src/main/java/com/nexusfin/equity/service/impl/BenefitsServiceImpl.java` and `src/main/java/com/nexusfin/equity/controller/BenefitsController.java` to enforce agreement validation and protocol-readiness checks before activation
- [ ] T025 [US1] Implement benefit order sync in `src/main/java/com/nexusfin/equity/service/impl/BenefitsServiceImpl.java` using the new benefit-sync contract after successful activation
- [ ] T026 [US1] Update `specs/004-benefit-loan-flow/contracts/page-flow-api.md` and `docs/third-part/科技平台/科技平台-小花接口文档-艾博生4.22_整理分析.md` with the final benefits-page field mapping and remaining open questions for benefit sync path

**Checkpoint**: User Story 1 is independently functional and aligned to the latest protocol/card expectations.

---

## Phase 4: User Story 2 - 在艾博生查看正确的借款状态与结果 (Priority: P1)

**Goal**: Align loan apply/query/status/result handling with Xiaohua 4.22 semantics while preserving the current H5-facing API shape.

**Independent Test**: Existing loan endpoints still work for H5, but status/result mapping now reflects `7001/7002/7003` correctly and the service can expose richer Xiaohua fields.

### Tests for User Story 2

- [ ] T027 [P] [US2] Add approval status/result mapping coverage in `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- [ ] T028 [P] [US2] Add controller integration coverage for approval endpoints in `src/test/java/com/nexusfin/equity/controller/LoanControllerIntegrationTest.java`
- [ ] T029 [P] [US2] Add callback processing coverage for latest grant-result semantics in `src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java`

### Implementation for User Story 2

- [ ] T030 [US2] Update `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java` to correct upstream status mapping so `7001=success`, `7002=processing`, and `7003=failure`
- [ ] T031 [US2] Extend `src/main/java/com/nexusfin/equity/dto/request/LoanApplyRequest.java` and related internal mapping code in `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java` to support richer Xiaohua apply fields including `loanReason`, `bankCardNum`, `basicInfo`, `idInfo`, `contactInfo`, `supplementInfo`, `optionInfo`, `imageInfo`, and `platformBenefitOrderNo`
- [ ] T032 [US2] Add repay-plan query support to `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java` and related DTOs so current loan result views can expose complete repayment schedule when needed
- [ ] T033 [US2] Update `src/main/java/com/nexusfin/equity/controller/NotificationCallbackController.java`, `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java`, and loan-related callback DTOs to align grant-result callback handling with Xiaohua 4.22 fields and latest logging semantics
- [ ] T034 [US2] Create or update alignment documentation in `docs/plan/20260424_小花4.22现有实现差异分析与开发计划.md` and `specs/004-benefit-loan-flow/contracts/tech-platform-outbound.md` for loan status mapping, callback fields, and unresolved Yunka state-machine dependencies

**Checkpoint**: Loan status and result pages are aligned to Xiaohua 4.22 semantics even before the future “云卡先建单” mainline is fully coded.

---

## Phase 5: User Story 3 - 在艾博生完成短信确认还款 (Priority: P1)

**Goal**: Expand the current repayment flow from direct trial/apply/query into a full repayment flow with card selection and sms confirmation.

**Independent Test**: A user can obtain repayment info, select a bound card, send sms, confirm sms, submit repayment, and query the final result through ABS endpoints.

### Tests for User Story 3

- [ ] T035 [P] [US3] Add service coverage for repayment sms send/confirm and result mapping in `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`
- [ ] T036 [P] [US3] Add controller integration coverage in `src/test/java/com/nexusfin/equity/controller/RepaymentControllerIntegrationTest.java`
- [ ] T037 [P] [US3] Add callback processing coverage for latest repayment-result semantics in `src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java`

### Implementation for User Story 3

- [ ] T038 [US3] Add repayment sms request and response DTOs under `src/main/java/com/nexusfin/equity/dto/request/` and `src/main/java/com/nexusfin/equity/dto/response/`
- [ ] T039 [US3] Refactor `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java` to query bound cards through `XiaohuaGatewayService` and expose them in repayment info responses
- [ ] T040 [US3] Extend `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java` and `src/main/java/com/nexusfin/equity/controller/RepaymentController.java` with sms-send and sms-confirm endpoints or endpoint-compatible service methods according to the current H5 contract decision
- [ ] T041 [US3] Update `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java` to align repayment submit/query semantics with Xiaohua 4.22 fields including `swiftNumber`, card context, and pending/success/failure states
- [ ] T042 [US3] Update repayment callback DTOs, `src/main/java/com/nexusfin/equity/controller/NotificationCallbackController.java`, and `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java` for latest repayment callback field mapping and idempotent processing

**Checkpoint**: Repayment flow is independently functional with sms confirmation and updated callback semantics.

---

## Phase 6: Deferred Mainline Refactor (Depends on External Yunka Contract)

**Purpose**: Prepare the future refactor for the `2026-04-23` confirmed mainline without prematurely hard-coding an unstable external state machine.

- [ ] T043 [SHARED] Create a design note under `docs/plan/` describing how current `/api/loan/apply` should evolve into “云卡建放款订单 -> 权益成功 -> 触发小花放款” once Yunka contract details are finalized
- [ ] T044 [SHARED] Identify required persistence or mapping changes for Yunka loan-order identifiers in the appropriate entity/repository files under `src/main/java/com/nexusfin/equity/entity/` and `src/main/java/com/nexusfin/equity/repository/`
- [ ] T045 [SHARED] Define the split between Yunka internal order status and Xiaohua final lending status in alignment docs and H5-facing response mapping notes

**Checkpoint**: Future mainline refactor is clearly designed but not prematurely implemented.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Finish cross-story consistency, documentation sync, and verification.

- [ ] T046 [P] Sync `specs/004-benefit-loan-flow/plan.md`, `specs/004-benefit-loan-flow/research.md`, `specs/004-benefit-loan-flow/data-model.md`, `specs/004-benefit-loan-flow/quickstart.md`, and the latest docs under `docs/plan/` with final field mappings and boundary decisions
- [ ] T047 [P] Run targeted tests for `BenefitsServiceTest`, `LoanServiceTest`, `RepaymentServiceTest`, `NotificationServiceTest`, and corresponding controller integration tests, then record the results in `specs/004-benefit-loan-flow/quickstart.md`
- [ ] T048 Run `mvn -q test`, `mvn -q clean package -DskipTests`, and `mvn -q checkstyle:check` once story work is complete, then record results in `specs/004-benefit-loan-flow/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Historical Baseline (Phase 1)**: Completed record only
- **Shared Alignment Foundation (Phase 2)**: Blocks all story alignment work
- **User Stories (Phase 3-5)**: Depend on Shared Foundation completion
- **Deferred Mainline Refactor (Phase 6)**: Depends on external Yunka contract details; can be documented before full implementation
- **Polish (Phase 7)**: Depends on desired story work completion

### User Story Dependencies

- **US1**: Starts after Shared Foundation and is the recommended first implementation slice
- **US2**: Starts after Shared Foundation; can overlap with US1 after shared facade is stable
- **US3**: Starts after Shared Foundation; can overlap with US2 if callback DTO ownership is coordinated

### Within Each Story

- Tests before implementation
- Shared DTOs before service logic
- Service logic before controller exposure
- Callback DTO changes before callback-processing refactors
- Docs and field mapping notes updated before final validation

### Parallel Opportunities

- `T013` and `T014` can run in parallel
- `T016` and `T018` can run in parallel after `T015`
- `T020` and `T021` can run in parallel inside US1
- `T027`, `T028`, and `T029` can run in parallel inside US2
- `T035`, `T036`, and `T037` can run in parallel inside US3
- `T046` and `T047` can run in parallel in the polish phase

---

## Implementation Strategy

### Recommended MVP Order

1. Finish Phase 2: Shared Alignment Foundation
2. Finish Phase 3: User Story 1
3. Finish the status-mapping portion of Phase 4 so approval pages stop showing wrong meanings
4. Finish Phase 5 for repayment sms/card closure

### Alignment-First Strategy

1. Shared facade and typed DTO first
2. Benefits dynamicization second
3. Loan status/apply/callback alignment third
4. Repayment sms/card/callback alignment fourth
5. External Yunka contract-driven mainline refactor last

### Parallel Team Strategy

1. Developer A: Shared Foundation + US1
2. Developer B: US2 loan alignment
3. Developer C: US3 repayment alignment + callback integration
4. Merge after shared DTO/facade contracts stabilize

---

## Notes

- Tasks now reflect the **current code baseline**, not the older assumption that all H5 APIs were still missing.
- `TechPlatformClient` is treated as legacy/non-mainline for this feature and should not be expanded as the primary Xiaohua 4.22 integration surface.
- The most urgent correctness bug is the current loan status mapping conflict with Xiaohua 4.22 (`7003` currently misread as success).
- The most important design constraint is still: **艾博生通过云卡调用小花，小花通过云卡回调艾博生**.
