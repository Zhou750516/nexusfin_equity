# Tasks: NexusFin Cross-Domain Auth

**Input**: Design document from `/docs/NexusFin_跨域认证设计方案.md` and current repository structure in `/src/`
**Prerequisites**: Manual fallback generation on `main` because spec-kit branch-gated scripts were intentionally skipped for this repository stage

**Tests**: Tasks include the verification required by the constitution. Core auth/session logic needs unit tests, and API or filter changes need integration coverage before feature completion.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Include exact file paths in every task description

## Path Conventions

- Application code: `src/main/java/com/nexusfin/equity/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/com/nexusfin/equity/` and `src/test/resources/`
- Feature tasks: `specs/002-cross-domain-auth/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare auth-related dependencies, properties, and schema support shared by all stories.

- [X] T001 Update auth-related dependencies and build configuration for local JWT processing and HTTP client support in `pom.xml`
- [X] T002 [P] Add tech-platform SSO endpoint, JWT secret, cookie settings, and redirect whitelist properties in `src/main/resources/application.yml` and `src/test/resources/application.yml`
- [X] T003 [P] Extend member schema with `tech_platform_user_id` and related indexes in `src/main/resources/db/schema.sql` and `src/test/resources/db/schema-h2.sql`
- [X] T004 [P] Seed auth-related test data and environment defaults in `src/test/resources/db/test-data.sql` and `src/test/resources/application-mysql-it.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared JWT, cookie, filter, repository, and error-handling infrastructure that all auth stories depend on.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T005 Create shared auth utilities for JWT generation/parsing, cookie writing, and current-user context in `src/main/java/com/nexusfin/equity/util/JwtUtil.java`, `src/main/java/com/nexusfin/equity/util/CookieUtil.java`, and `src/main/java/com/nexusfin/equity/util/AuthContextUtil.java`
- [X] T006 [P] Add auth configuration and request filtering in `src/main/java/com/nexusfin/equity/config/AuthProperties.java`, `src/main/java/com/nexusfin/equity/config/JwtAuthenticationFilter.java`, and `src/main/java/com/nexusfin/equity/config/WebMvcConfig.java`
- [X] T007 [P] Extend member persistence for tech-platform identity lookup in `src/main/java/com/nexusfin/equity/entity/MemberInfo.java` and `src/main/java/com/nexusfin/equity/repository/MemberInfoRepository.java`
- [X] T008 Update auth-related exception mapping and unauthorized response handling in `src/main/java/com/nexusfin/equity/exception/BizException.java` and `src/main/java/com/nexusfin/equity/exception/GlobalExceptionHandler.java`
- [X] T009 [P] Add foundational unit tests for JWT parsing, cookie flags, and auth filter behavior in `src/test/java/com/nexusfin/equity/util/JwtUtilTest.java`, `src/test/java/com/nexusfin/equity/util/CookieUtilTest.java`, and `src/test/java/com/nexusfin/equity/config/JwtAuthenticationFilterTest.java`

**Checkpoint**: Foundation is ready. User stories can now be implemented with shared session and auth enforcement rules in place.

---

## Phase 3: User Story 1 - 科技平台跳转自动登录与即时建档 (Priority: P1) 🎯 MVP

**Goal**: Accept a tech-platform token through the SSO callback, verify it server-to-server, create or reuse the ABS-side member, and issue the local login cookie.

**Independent Test**: Call `GET /api/auth/sso-callback?token=...` with a mocked upstream verification result and confirm the service creates or reuses one `member_info` row, sets the local JWT cookie, and redirects to the allowed page.

### Tests for User Story 1

- [X] T010 [P] [US1] Add service tests for upstream token verification and JIT member provisioning in `src/test/java/com/nexusfin/equity/service/AuthServiceTest.java`
- [X] T011 [P] [US1] Add integration tests for `GET /api/auth/sso-callback` redirect, whitelist validation, and cookie issuance in `src/test/java/com/nexusfin/equity/controller/AuthControllerIntegrationTest.java`

### Implementation for User Story 1

- [X] T012 [P] [US1] Create auth DTOs for upstream user profile and local current-user view in `src/main/java/com/nexusfin/equity/dto/response/TechPlatformUserProfileResponse.java` and `src/main/java/com/nexusfin/equity/dto/response/CurrentUserResponse.java`
- [X] T013 [P] [US1] Implement the tech-platform user verification client in `src/main/java/com/nexusfin/equity/service/TechPlatformUserClient.java` and `src/main/java/com/nexusfin/equity/service/impl/TechPlatformUserClientImpl.java`
- [X] T014 [US1] Implement SSO callback login orchestration and member JIT provisioning in `src/main/java/com/nexusfin/equity/service/AuthService.java` and `src/main/java/com/nexusfin/equity/service/impl/AuthServiceImpl.java`
- [X] T015 [US1] Implement the SSO callback redirect endpoint in `src/main/java/com/nexusfin/equity/controller/AuthController.java`

**Checkpoint**: User Story 1 is complete when a browser-style redirect request can bootstrap an ABS-side logged-in session without calling `/api/users/register`.

---

## Phase 4: User Story 2 - 本地会话态查询与业务接口登录保护 (Priority: P2)

**Goal**: Expose `/api/users/me` for front-end login-state checks and require valid local JWT cookies on ABS business APIs while keeping partner callbacks on signature-based auth.

**Independent Test**: Call `GET /api/users/me` with and without the ABS JWT cookie, then call `/api/equity/products/{productCode}` and `POST /api/equity/orders` to confirm authenticated access succeeds and unauthenticated access is rejected.

### Tests for User Story 2

- [X] T016 [P] [US2] Add integration tests for `/api/users/me` and JWT-protected business endpoints in `src/test/java/com/nexusfin/equity/controller/AuthControllerIntegrationTest.java` and `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`
- [X] T017 [P] [US2] Add service tests for authenticated member resolution in product and order flows in `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceAuthTest.java`

### Implementation for User Story 2

- [X] T018 [US2] Implement current-user lookup and `/api/users/me` response assembly in `src/main/java/com/nexusfin/equity/controller/AuthController.java` and `src/main/java/com/nexusfin/equity/service/impl/AuthServiceImpl.java`
- [X] T019 [US2] Refactor product and order APIs to derive the acting member from auth context instead of explicit `memberId` input in `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java`, `src/main/java/com/nexusfin/equity/service/BenefitOrderService.java`, `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`, and `src/main/java/com/nexusfin/equity/dto/request/CreateBenefitOrderRequest.java`
- [X] T020 [US2] Split auth boundaries so `/api/equity/**` and `/api/users/me` require local JWT while `/api/callbacks/**` keeps signature validation in `src/main/java/com/nexusfin/equity/config/JwtAuthenticationFilter.java`, `src/main/java/com/nexusfin/equity/config/SignatureInterceptor.java`, and `src/main/java/com/nexusfin/equity/config/WebMvcConfig.java`
- [X] T021 [US2] Add trace logging for authenticated member access and login-state checks in `src/main/java/com/nexusfin/equity/controller/AuthController.java` and `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java`
  - Note: `AuthController` 已补齐 SSO 登录和 `/api/users/me` 日志；`BenefitOrderController` 已补齐产品页、下单、查单、行权链接访问日志，并新增集成测试覆盖日志输出。

**Checkpoint**: User Story 2 is complete when front-end pages can distinguish logged-in versus guest access using `/api/users/me`, and order/product endpoints no longer rely on externally supplied member identifiers.

---

## Phase 5: User Story 3 - 取消静默注册并同步存量接口文档 (Priority: P3)

**Goal**: Remove the obsolete explicit registration flow and align repository contracts, smoke tests, and business documents with the new SSO + JWT model.

**Independent Test**: Confirm the old `/api/users/register` flow is no longer part of the public contract, the quickstart/regression tests cover the new auth-first flow, and docs no longer describe silent registration as the entrypoint.

### Tests for User Story 3

- [X] T022 [P] [US3] Replace silent-registration regression coverage with auth-first onboarding tests in `src/test/java/com/nexusfin/equity/controller/UserRegistrationControllerIntegrationTest.java`, `src/test/java/com/nexusfin/equity/controller/AuthControllerIntegrationTest.java`, and `src/test/java/com/nexusfin/equity/NexusfinEquityApplicationTests.java`
  - Note: 旧的 `UserRegistrationControllerIntegrationTest` 随 `/api/users/register` 下线而移除，等价覆盖已由 `AuthControllerIntegrationTest` 与 `NexusfinEquityApplicationTests` 承接。

### Implementation for User Story 3

- [X] T023 [P] [US3] Remove obsolete registration controller, service, and DTO classes in `src/main/java/com/nexusfin/equity/controller/UserRegistrationController.java`, `src/main/java/com/nexusfin/equity/service/MemberOnboardingService.java`, `src/main/java/com/nexusfin/equity/service/impl/MemberOnboardingServiceImpl.java`, `src/main/java/com/nexusfin/equity/dto/request/RegisterUserRequest.java`, and `src/main/java/com/nexusfin/equity/dto/response/RegisterUserResponse.java`
- [X] T024 [US3] Update public API and callback documentation to reflect SSO callback, `/api/users/me`, and auth-first order entry in `docs/API_STANDARD_20260323.md`, `docs/接口业务处理逻辑_20260324.md`, and `docs/接口业务逻辑Review_20260324.md`
  - Note: 当前仓库实际落地的评审同步说明位于 `docs/接口业务逻辑Review_20260326_修改记录.md`，与任务编写时预期的 `_20260324.md` 文件名存在漂移，但文档内容已同步到 auth-first 模型。
- [X] T025 [US3] Refresh baseline contract and smoke-flow artifacts for the new auth model in `specs/001-equity-service-baseline/contracts/abs-public-api.yaml`, `specs/001-equity-service-baseline/quickstart.md`, and `src/test/resources/db/test-data.sql`

**Checkpoint**: User Story 3 is complete when the repository no longer presents silent registration as the standard entrypoint and all local documents match the new cross-domain auth design.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish security hardening, observability, and end-to-end verification across all stories.

- [X] T026 [P] Add timeout, retry, and failure-observability hardening for upstream user verification in `src/main/java/com/nexusfin/equity/service/impl/TechPlatformUserClientImpl.java` and `src/main/resources/application.yml`
  - Note: 已补齐 `retry-max-attempts` 配置、最小重试策略、成功/失败日志留痕，并新增 `TechPlatformUserClientImplTest` 覆盖重试成功与重试耗尽场景。
- [X] T027 Add redirect whitelist, cookie `HttpOnly/Secure/SameSite`, and URL cleanup notes to delivery docs in `docs/NexusFin_跨域认证设计方案.md` and `docs/CODE_MODIFY_20260323.md`
- [X] T028 [P] Add end-to-end auth smoke coverage for SSO callback, `/api/users/me`, and protected order creation in `src/test/java/com/nexusfin/equity/NexusfinEquityApplicationTests.java`
- [X] T029 Run and record verification for `mvn test`, `mvn clean package -DskipTests`, and `mvn checkstyle:check` in `docs/CODE_MODIFY_20260323.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no prerequisites and can start immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all story work.
- **Phase 3: US1** depends on Phase 2.
- **Phase 4: US2** depends on US1 for the newly issued local session and member context.
- **Phase 5: US3** depends on US1 and US2 because it removes the old onboarding entrypoint only after the new auth-first flow is in place.
- **Phase 6: Polish** depends on the stories you intend to ship.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories after foundational work.
- **US2 (P2)**: Depends on US1 because `/api/users/me` and protected business APIs rely on the local JWT established by the SSO callback.
- **US3 (P3)**: Depends on US1 and US2 because silent registration should only be removed after the replacement path is stable.

### Within Each User Story

- Write tests first and verify they fail before implementation.
- Complete DTO and repository work before service logic.
- Complete service logic before controller/filter wiring.
- Remove the legacy registration path only after the auth-first path passes smoke coverage.

### Parallel Opportunities

- `T002`, `T003`, and `T004` can run in parallel after `T001`.
- `T006`, `T007`, and `T009` can run in parallel after `T005`.
- `T010` and `T011` can run in parallel for US1.
- `T012` and `T013` can run in parallel for US1.
- `T016` and `T017` can run in parallel for US2.
- `T022` and `T023` can run in parallel for US3 if file ownership is coordinated.
- `T026` and `T028` can run in parallel during polish.

---

## Parallel Example: User Story 1

```bash
Task T010: Add service tests in src/test/java/com/nexusfin/equity/service/AuthServiceTest.java
Task T011: Add callback integration tests in src/test/java/com/nexusfin/equity/controller/AuthControllerIntegrationTest.java

Task T012: Create auth DTOs in src/main/java/com/nexusfin/equity/dto/response/TechPlatformUserProfileResponse.java
Task T013: Implement upstream verification client in src/main/java/com/nexusfin/equity/service/impl/TechPlatformUserClientImpl.java
```

## Parallel Example: User Story 2

```bash
Task T016: Add /api/users/me and protected-endpoint integration tests in src/test/java/com/nexusfin/equity/controller/AuthControllerIntegrationTest.java
Task T017: Add authenticated order-flow service tests in src/test/java/com/nexusfin/equity/service/BenefitOrderServiceAuthTest.java
```

## Parallel Example: User Story 3

```bash
Task T022: Replace silent-registration regression tests in src/test/java/com/nexusfin/equity/controller/UserRegistrationControllerIntegrationTest.java
Task T024: Update API and business-flow docs in docs/API_STANDARD_20260323.md
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Validate the SSO callback can create/reuse the member and issue the local cookie.
5. Stop and review before protecting all business APIs.

### Incremental Delivery

1. Deliver US1 for callback-driven login and JIT provisioning.
2. Deliver US2 for local session-state querying and auth-protected business APIs.
3. Deliver US3 for legacy registration removal and document cleanup.
4. Finish with hardening, smoke verification, and delivery notes.

### Suggested MVP Scope

- Ship through **User Story 1** first.
- Keep **User Story 2** behind the same auth foundation once the callback login is stable.
- Remove `/api/users/register` only after **User Story 2** passes end-to-end verification.
