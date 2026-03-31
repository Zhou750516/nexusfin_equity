# Tasks: 惠选卡权益与借款流程 V1 页面交互理解

**Input**: Design documents from `/specs/004-benefit-loan-flow/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tasks include the verification needed by the constitution. Core business methods require unit tests, and API or persistence changes add controller or integration coverage where needed.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g. `US1`, `US2`, `US3`)
- Include exact file paths in descriptions

## Phase 1: Setup (Design Baseline & Task Inputs)

**Purpose**: 补齐当前 feature 的设计输入，让后续开发任务有明确边界，且把“先做 thirdparty 科技平台调用层”的顺序固定下来。

- [X] T001 Update `specs/004-benefit-loan-flow/plan.md` with actual technical context, thirdparty-first sequencing, constitution gates, and verification commands
- [X] T002 Create `specs/004-benefit-loan-flow/research.md` from `docs/科技平台接口-业务流程分析合理性评审_20260330.md` and `docs/科技平台导流对接标准接口文档.docx`
- [X] T003 [P] Create `specs/004-benefit-loan-flow/data-model.md` for equity page view, loan status view, repayment view, and tech-platform outbound envelope
- [X] T004 [P] Create `specs/004-benefit-loan-flow/contracts/page-flow-api.md` and `specs/004-benefit-loan-flow/contracts/tech-platform-outbound.md`
- [X] T005 Create `specs/004-benefit-loan-flow/quickstart.md` with local run, callback simulation, and Maven verification steps

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 先完成所有故事共享的科技平台 thirdparty 调用底座、配置和文档，这一阶段完成前不进入任何页面流程实现。

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 Create `src/main/java/com/nexusfin/equity/config/TechPlatformProperties.java` and wire defaults in `src/main/resources/application.yml`
- [X] T007 [P] Create shared envelope, signature, and crypto support classes under `src/main/java/com/nexusfin/equity/thirdparty/techplatform/`
- [X] T008 [P] Create request and response models for outbound tech-platform APIs under `src/main/java/com/nexusfin/equity/thirdparty/techplatform/`
- [X] T009 Create `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClient.java` and `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClientImpl.java`
- [X] T010 [P] Add client codec and response parsing tests in `src/test/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClientTest.java`
- [X] T011 [P] Update mock and test configuration in `src/test/resources/application.yml` and `src/test/resources/application-mysql-it.yml` for tech-platform client properties
- [X] T012 Create `docs/科技平台接口调用实现说明_20260330.md` to document scope, implemented APIs, signing/encryption rules, and deferred APIs

**Checkpoint**: Foundation ready - downstream page and callback work can now begin

---

## Phase 3: User Story 1 - 在艾博生完成权益开通前确认 (Priority: P1) 🎯 MVP

**Goal**: 支撑艾博生权益首页所需的数据展示与开通前校验，包括权益信息、规则说明、支付卡信息和协议勾选后的开通动作。

**Independent Test**: 调用权益页相关接口后，能够独立得到权益信息、规则说明、支付卡摘要，并在协议未勾选时阻止开通。

### Tests for User Story 1

- [ ] T013 [P] [US1] Add controller coverage for equity page and payment-card endpoints in `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`
- [ ] T014 [P] [US1] Add service coverage for equity page aggregation and agreement gating in `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`

### Implementation for User Story 1

- [ ] T015 [P] [US1] Create page response models in `src/main/java/com/nexusfin/equity/dto/response/EquityInfoResponse.java`, `src/main/java/com/nexusfin/equity/dto/response/EquityRuleResponse.java`, and `src/main/java/com/nexusfin/equity/dto/response/PaymentCardResponse.java`
- [ ] T016 [P] [US1] Extend query support in `src/main/java/com/nexusfin/equity/repository/BenefitProductRepository.java` and `src/main/java/com/nexusfin/equity/repository/MemberInfoRepository.java`
- [ ] T017 [US1] Extend `src/main/java/com/nexusfin/equity/service/BenefitOrderService.java` and `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java` to provide equity info, rules, payment card summary, and agreement gate checks
- [ ] T018 [US1] Extend `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java` and create `src/main/java/com/nexusfin/equity/controller/UserProfileController.java` for `/api/equity/info`, `/api/equity/rules`, and `/api/users/payment-card`
- [ ] T019 [US1] Add `Result<T>` wrapping, auth checks, `@Valid` usage, and `traceId + bizOrderNo` logging in `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java`, `src/main/java/com/nexusfin/equity/controller/UserProfileController.java`, and `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - 在云卡确认借款并查看审核结果 (Priority: P2)

**Goal**: 打通借款试算、借款审核状态展示和放款结果同步所需的 ABS 侧能力，重点先完成科技平台 thirdparty 调用和状态同步闭环。

**Independent Test**: 在已有订单基础上，能够独立完成借款试算数据准备、审核状态查询和放款结果通知处理，不依赖还款流程。

### Tests for User Story 2

- [ ] T020 [P] [US2] Add grant-result and loan-status propagation tests in `src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java`
- [ ] T021 [P] [US2] Add controller or integration coverage for order status polling in `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java` and `src/test/java/com/nexusfin/equity/controller/NotificationCallbackControllerIntegrationTest.java`

### Implementation for User Story 2

- [ ] T022 [P] [US2] Create loan-trial, loan-status, and receiving-card DTOs in `src/main/java/com/nexusfin/equity/dto/request/LoanTrialRequest.java`, `src/main/java/com/nexusfin/equity/dto/response/LoanTrialResponse.java`, `src/main/java/com/nexusfin/equity/dto/response/LoanStatusResponse.java`, and `src/main/java/com/nexusfin/equity/dto/response/ReceivingCardResponse.java`
- [ ] T023 [P] [US2] Add tech-platform loan APIs to `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClient.java` and related models under `src/main/java/com/nexusfin/equity/thirdparty/techplatform/`
- [ ] T024 [US2] Implement loan notification, loan status query, and receiving-card aggregation in `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClientImpl.java`, `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java`, and `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- [ ] T025 [US2] Extend `src/main/java/com/nexusfin/equity/dto/response/BenefitOrderStatusResponse.java` and `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java` for polling-friendly loan status output
- [ ] T026 [US2] Document downstream YunKa and tech-platform borrow flow mapping in `docs/科技平台接口调用实现说明_20260330.md` and `specs/004-benefit-loan-flow/contracts/tech-platform-outbound.md`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - 在艾博生完成还款确认 (Priority: P3)

**Goal**: 支撑从科技平台还款入口进入艾博生还款页后的试算、身份验证和还款结果同步能力。

**Independent Test**: 只依赖已存在订单，即可独立验证还款试算、身份验证前置校验和还款结果处理。

### Tests for User Story 3

- [ ] T027 [P] [US3] Add repayment callback and repayment sync tests in `src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java`
- [ ] T028 [P] [US3] Add repayment API integration coverage in `src/test/java/com/nexusfin/equity/controller/NotificationCallbackControllerIntegrationTest.java` and `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`

### Implementation for User Story 3

- [ ] T029 [P] [US3] Create repayment request and response models in `src/main/java/com/nexusfin/equity/dto/request/RepaymentTrialRequest.java`, `src/main/java/com/nexusfin/equity/dto/request/RepaymentConfirmRequest.java`, `src/main/java/com/nexusfin/equity/dto/response/RepaymentTrialResponse.java`, and `src/main/java/com/nexusfin/equity/dto/response/RepaymentConfirmResponse.java`
- [ ] T030 [P] [US3] Add repayment APIs, sms auth APIs, and entry-url APIs to `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClient.java` and related models under `src/main/java/com/nexusfin/equity/thirdparty/techplatform/`
- [ ] T031 [US3] Create `src/main/java/com/nexusfin/equity/service/RepaymentService.java` and `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java` for repayment trial, auth-before-submit, and repay result handling
- [ ] T032 [US3] Create `src/main/java/com/nexusfin/equity/controller/RepaymentController.java` for `/api/repayment/trial` and confirm-repayment flow with `Result<T>` responses
- [ ] T033 [US3] Extend `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java` and `src/main/java/com/nexusfin/equity/dto/request/RepaymentForwardCallbackRequest.java` to persist repayment synchronization and trace logging

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 完成跨故事一致性、文档同步和全量验证。

- [X] T034 [P] Sync design artifacts in `specs/004-benefit-loan-flow/plan.md`, `specs/004-benefit-loan-flow/research.md`, `specs/004-benefit-loan-flow/data-model.md`, and `specs/004-benefit-loan-flow/quickstart.md` with final implementation decisions
- [X] T035 [P] Update business docs in `docs/科技平台接口调用实现说明_20260330.md` and `docs/科技平台接口-业务流程分析合理性评审_20260330.md` with implemented scope and remaining gaps
- [X] T036 Run `mvn -q test`, `mvn -q clean package -DskipTests`, and `mvn -q checkstyle:check`, then record results in `specs/004-benefit-loan-flow/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - no dependency on other stories
- **User Story 2 (P2)**: Can start after Foundational - reuses the shared tech-platform client and current order/callback chain
- **User Story 3 (P3)**: Can start after Foundational - depends on shared tech-platform client and repayment-specific service/controller work

### Within Each User Story

- Tests before implementation
- DTOs and repository helpers before service logic
- Service logic before controller exposure
- Thirdparty tech-platform methods before story-level orchestration
- Story complete before moving to next validation checkpoint

### Parallel Opportunities

- `T003` and `T004` can run in parallel after `T002`
- `T007`, `T008`, `T010`, and `T011` can run in parallel after `T006`
- `T013` and `T014` can run in parallel inside User Story 1
- `T020` and `T021` can run in parallel inside User Story 2
- `T022` and `T023` can run in parallel inside User Story 2
- `T027` and `T028` can run in parallel inside User Story 3
- `T029` and `T030` can run in parallel inside User Story 3
- `T034` and `T035` can run in parallel in the polish phase

---

## Parallel Example: User Story 2

```bash
# Tests for loan result flow
Task: "Add grant-result propagation coverage in src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java"
Task: "Add polling/status integration coverage in src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java"

# Models and client contract work
Task: "Create loan DTOs in src/main/java/com/nexusfin/equity/dto/request/LoanTrialRequest.java and src/main/java/com/nexusfin/equity/dto/response/LoanTrialResponse.java"
Task: "Add loan-related thirdparty models under src/main/java/com/nexusfin/equity/thirdparty/techplatform/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. Stop and validate User Story 1 endpoints independently

### Thirdparty-First Delivery

1. Finish Setup and Foundational phases to land the tech-platform client, config, docs, and tests
2. Add User Story 1 to expose the ABS equity page support endpoints
3. Add User Story 2 to complete borrow-status and grant synchronization
4. Add User Story 3 to complete repayment-side orchestration

### Parallel Team Strategy

1. One developer completes Phase 1 and Phase 2
2. After the shared tech-platform client is stable:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Merge in story order with validation at each checkpoint

---

## Notes

- `[P]` tasks target different files and can be parallelized safely
- `US1` is the suggested MVP scope
- The current repository is ABS-side, so tasks prioritize ABS-owned endpoints and outbound tech-platform integration rather than embedding YunKa runtime responsibilities directly
- `plan.md` is still template-heavy today, so the first setup tasks intentionally include design artifact completion before code implementation
