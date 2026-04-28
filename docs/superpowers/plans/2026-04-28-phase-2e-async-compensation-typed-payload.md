# Phase 2E Async Compensation Typed Payload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace inline JSON string construction in async compensation producers with strongly typed payload objects while keeping the persisted `request_payload` JSON shape and all current compensation execution semantics unchanged.

**Architecture:** Introduce a dedicated typed payload model for `AsyncCompensationEnqueueService.EnqueueCommand`, and make `AsyncCompensationEnqueueServiceImpl` responsible for serializing that payload into the existing `request_payload` database column. Both current producers, `LoanApplicationServiceImpl` and `BenefitOrderServiceImpl`, will switch from handwritten text blocks to typed payload objects. Consumer-side production code, including `YunkaLoanApplyCompensationExecutor`, `QwBenefitPurchaseCompensationExecutor`, `AsyncCompensationWorkerServiceImpl`, schema, and repositories, stays unchanged and continues to deserialize the same JSON shape from storage.

**Tech Stack:** Java 17, Spring Boot 3.2, Jackson, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueuePayload.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueueService.java`
- `src/main/java/com/nexusfin/equity/service/impl/AsyncCompensationEnqueueServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`
- `src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueueService.java`
- `src/main/java/com/nexusfin/equity/service/impl/AsyncCompensationEnqueueServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`
- `src/main/java/com/nexusfin/equity/service/impl/QwBenefitPurchaseCompensationExecutor.java`
- `src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java`
- `src/test/java/com/nexusfin/equity/service/QwBenefitPurchaseCompensationExecutorTest.java`
- `src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java`

### Explicitly In Scope

- add typed payload types for current async compensation producers
- change `AsyncCompensationEnqueueService.EnqueueCommand.requestPayload` from `String` to a typed payload object
- serialize typed payloads to JSON inside `AsyncCompensationEnqueueServiceImpl`
- switch `LoanApplicationServiceImpl` to typed payload enqueue
- switch `BenefitOrderServiceImpl` to typed payload enqueue
- update producer tests and enqueue service tests accordingly
- keep stored `request_payload` JSON shape compatible with current executors

### Explicitly Out of Scope

- any `AsyncCompensationTask` schema change
- any `AsyncCompensationAttempt` schema change
- any `AsyncCompensationWorkerServiceImpl` production change
- any `YunkaLoanApplyCompensationExecutor` production change
- any `QwBenefitPurchaseCompensationExecutor` production change
- moving payload records into `thirdparty/.../payload/`
- any `RepaymentServiceImpl` change
- any `.gitignore` change
- introducing a generic payload registry or plugin system

### Boundary Notes

- Persisted `request_payload` remains a JSON string in the database.
- `AsyncCompensationEnqueueServiceImpl` becomes the only place responsible for turning typed payloads into stored JSON text.
- Keep the JSON field names exactly compatible with current consumers:
  - Yunka loan apply retry payload fields remain:
    - `requestId`
    - `path`
    - `bizOrderNo`
    - `uid`
    - `benefitOrderNo`
    - `applyId`
    - `loanId`
    - `loanAmount`
    - `loanPeriod`
    - `bankCardNo`
  - QW benefit purchase retry payload fields remain:
    - `externalUserId`
    - `benefitOrderNo`
    - `productCode`
    - `loanAmount`
- Keep `LoanApplicationServiceImpl` timeout behavior unchanged:
  - still returns `pending`
  - still enqueues `YUNKA_LOAN_APPLY_RETRY`
  - still persists the same biz key / request path / method
- Keep `BenefitOrderServiceImpl` timeout behavior unchanged:
  - still enqueues `QW_BENEFIT_PURCHASE_RETRY`
  - still throws `BenefitPurchaseSyncTimeoutCompensationException`
- Do not modify executor-local payload records in this phase; they serve as compatibility verification.

### Validation Commands

- `mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest test`
- `mvn -Dtest=YunkaLoanApplyCompensationExecutorTest,QwBenefitPurchaseCompensationExecutorTest test`
- `mvn -q -DskipTests compile`
- `rg -n '"""' src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- Optional if MySQL IT env is available:
  - `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlAsyncCompensationIntegrationTest test`

---

### Task 1: Freeze Typed Payload Producer Behavior with Focused Tests

**Files:**
- Modify: `src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java`
- Test: `mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest test`

- [ ] **Step 1: Update tests to express the typed payload contract before production code exists**

Make these concrete test changes.

In `src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java`, replace the string payloads with typed payload objects:

```java
service.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
        "YUNKA_LOAN_APPLY_RETRY",
        "LOAN_APPLY:APP-20260418-001",
        "APP-20260418-001",
        "YUNKA",
        "/api/gateway/proxy",
        "POST",
        null,
        new AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry(
                "LA-001",
                "/loan/apply",
                "APP-20260418-001",
                "user-001",
                "BEN-001",
                "APP-20260418-001",
                "LN-001",
                300000L,
                3,
                "acc_001"
        )
));
```

After capturing the inserted `AsyncCompensationTask`, add this exact persisted JSON assertion:

```java
assertThat(captor.getValue().getRequestPayload())
        .contains("\"path\":\"/loan/apply\"")
        .contains("\"bizOrderNo\":\"APP-20260418-001\"")
        .contains("\"loanAmount\":300000")
        .contains("\"loanPeriod\":3");
```

Also update the duplicate-task test to use the same typed payload object instead of a raw string.

In `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`, replace the timeout payload-string assertion:

```java
ArgumentCaptor<AsyncCompensationEnqueueService.EnqueueCommand> enqueueCaptor =
        ArgumentCaptor.forClass(AsyncCompensationEnqueueService.EnqueueCommand.class);
verify(asyncCompensationEnqueueService).enqueue(enqueueCaptor.capture());
assertThat(enqueueCaptor.getValue().taskType()).isEqualTo("YUNKA_LOAN_APPLY_RETRY");
assertThat(enqueueCaptor.getValue().bizKey()).isEqualTo("LOAN_APPLY:" + response.applicationId());
assertThat(enqueueCaptor.getValue().bizOrderNo()).isEqualTo(response.applicationId());
assertThat(enqueueCaptor.getValue().requestPath()).isEqualTo("/api/gateway/proxy");
assertThat(enqueueCaptor.getValue().requestPayload())
        .isInstanceOf(AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry.class);
AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry payload =
        (AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry) enqueueCaptor.getValue().requestPayload();
assertThat(payload.path()).isEqualTo("/loan/apply");
assertThat(payload.benefitOrderNo()).isEqualTo("BEN-TIMEOUT");
assertThat(payload.uid()).isEqualTo("user-001");
assertThat(payload.applyId()).isEqualTo(response.applicationId());
```

In `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`, strengthen the timeout test from `verify(asyncCompensationEnqueueService).enqueue(any());` to:

```java
ArgumentCaptor<AsyncCompensationEnqueueService.EnqueueCommand> enqueueCaptor =
        ArgumentCaptor.forClass(AsyncCompensationEnqueueService.EnqueueCommand.class);
verify(asyncCompensationEnqueueService).enqueue(enqueueCaptor.capture());
assertThat(enqueueCaptor.getValue().taskType()).isEqualTo("QW_BENEFIT_PURCHASE_RETRY");
assertThat(enqueueCaptor.getValue().bizKey()).startsWith("BENEFIT_PURCHASE:");
assertThat(enqueueCaptor.getValue().requestPayload())
        .isInstanceOf(AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry.class);
AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry payload =
        (AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry) enqueueCaptor.getValue().requestPayload();
assertThat(payload.externalUserId()).isEqualTo("user-4");
assertThat(payload.productCode()).isEqualTo("P-4");
assertThat(payload.loanAmount()).isEqualTo(680000L);
```

In `src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java`, replace the direct raw string enqueue call with:

```java
enqueueService.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
        "YUNKA_LOAN_APPLY_RETRY",
        bizKey,
        bizOrderNo,
        "YUNKA",
        "/api/gateway/proxy",
        "POST",
        null,
        new AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry(
                "LA-MYSQL-001",
                "/loan/apply",
                bizOrderNo,
                "mysql-it-user",
                "BEN-MYSQL-001",
                bizOrderNo,
                "LN-MYSQL-001",
                300000L,
                3,
                "acc_001"
        )
));
```

Then add:

```java
assertThat(task.getRequestPayload()).contains("\"path\":\"/loan/apply\"");
assertThat(task.getRequestPayload()).contains("\"bizOrderNo\":\"" + bizOrderNo + "\"");
```

- [ ] **Step 2: Run the focused tests and confirm the red phase is only missing typed payload contract changes**

Run:

```bash
mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest test
```

Expected red phase:

- `AsyncCompensationEnqueuePayload` does not exist
- or `EnqueueCommand` still expects `String requestPayload`

- [ ] **Step 3: Commit the red tests only**

```bash
git add src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java \
        src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java \
        src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java \
        src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java
git commit -m "test: freeze async compensation typed payload behavior"
```

---

### Task 2: Introduce Typed Payload Contract and Rewire Current Producers

**Files:**
- Create: `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueuePayload.java`
- Modify: `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueueService.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/AsyncCompensationEnqueueServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java`
- Test: `mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest test`

- [ ] **Step 1: Add the typed payload model**

Create `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueuePayload.java`:

```java
package com.nexusfin.equity.service;

public sealed interface AsyncCompensationEnqueuePayload
        permits AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry,
                AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry {

    record YunkaLoanApplyRetry(
            String requestId,
            String path,
            String bizOrderNo,
            String uid,
            String benefitOrderNo,
            String applyId,
            String loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo
    ) implements AsyncCompensationEnqueuePayload {
    }

    record QwBenefitPurchaseRetry(
            String externalUserId,
            String benefitOrderNo,
            String productCode,
            Long loanAmount
    ) implements AsyncCompensationEnqueuePayload {
    }
}
```

- [ ] **Step 2: Change the enqueue command contract from raw string to typed payload**

Update `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueueService.java`:

```java
package com.nexusfin.equity.service;

public interface AsyncCompensationEnqueueService {

    void enqueue(EnqueueCommand command);

    record EnqueueCommand(
            String taskType,
            String bizKey,
            String bizOrderNo,
            String targetCode,
            String requestPath,
            String httpMethod,
            String requestHeaders,
            AsyncCompensationEnqueuePayload requestPayload
    ) {
    }
}
```

- [ ] **Step 3: Make AsyncCompensationEnqueueServiceImpl serialize typed payloads into the existing database string field**

Update `src/main/java/com/nexusfin/equity/service/impl/AsyncCompensationEnqueueServiceImpl.java`:

- inject `ObjectMapper`
- serialize `command.requestPayload()` before writing `task.setRequestPayload(...)`
- keep all task metadata, retry defaults, partitioning, and duplicate handling unchanged

Use this concrete constructor shape:

```java
private final ObjectMapper objectMapper;

public AsyncCompensationEnqueueServiceImpl(
        AsyncCompensationTaskRepository taskRepository,
        AsyncCompensationPartitioner partitioner,
        AsyncCompensationProperties properties,
        ObjectMapper objectMapper
) {
    this.taskRepository = taskRepository;
    this.partitioner = partitioner;
    this.properties = properties;
    this.objectMapper = objectMapper;
}
```

Use this exact helper:

```java
private String serializePayload(AsyncCompensationEnqueuePayload payload) {
    try {
        return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
        throw new BizException("ASYNC_COMPENSATION_PAYLOAD_SERIALIZE_FAILED",
                "Failed to serialize async compensation payload");
    }
}
```

and set:

```java
task.setRequestPayload(serializePayload(command.requestPayload()));
```

- [ ] **Step 4: Rewire both current producers to construct typed payload objects instead of text blocks**

In `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`, replace the raw text block enqueue payload with:

```java
new AsyncCompensationEnqueueService.EnqueueCommand(
        "YUNKA_LOAN_APPLY_RETRY",
        "LOAN_APPLY:" + applicationId,
        applicationId,
        "YUNKA",
        yunkaProperties.gatewayPath(),
        "POST",
        null,
        new AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry(
                requestId,
                yunkaProperties.paths().loanApply(),
                applicationId,
                uid,
                benefitOrder.benefitOrderNo(),
                applicationId,
                loanId,
                yuanToCent(request.amount()),
                request.term(),
                upstreamBankCardNum
        )
)
```

In `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`, replace the raw text block enqueue payload with:

```java
new AsyncCompensationEnqueueService.EnqueueCommand(
        "QW_BENEFIT_PURCHASE_RETRY",
        "BENEFIT_PURCHASE:" + benefitOrder.getBenefitOrderNo(),
        benefitOrder.getBenefitOrderNo(),
        "QW",
        "/api/abs/method",
        "POST",
        null,
        new AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry(
                benefitOrder.getExternalUserId(),
                benefitOrder.getBenefitOrderNo(),
                product.getProductCode(),
                benefitOrder.getLoanAmount()
        )
)
```

Do not change any surrounding business logic, exception semantics, or task metadata.

- [ ] **Step 5: Run the focused producer/enqueue tests until they pass**

Run:

```bash
mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest test
```

Expected green phase:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 6: Commit the typed payload implementation**

```bash
git add src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueuePayload.java \
        src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueueService.java \
        src/main/java/com/nexusfin/equity/service/impl/AsyncCompensationEnqueueServiceImpl.java \
        src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java \
        src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/AsyncCompensationEnqueueServiceTest.java \
        src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java \
        src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java \
        src/test/java/com/nexusfin/equity/controller/MySqlAsyncCompensationIntegrationTest.java
git commit -m "refactor: add typed async compensation payloads"
```

---

### Task 3: Final Compatibility Verification Against Existing Consumers

**Files:**
- Verify only, no intended code change
- Test: `mvn -Dtest=YunkaLoanApplyCompensationExecutorTest,QwBenefitPurchaseCompensationExecutorTest test`
- Test: `mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest,YunkaLoanApplyCompensationExecutorTest,QwBenefitPurchaseCompensationExecutorTest test`
- Test: `mvn -q -DskipTests compile`

- [ ] **Step 1: Run executor compatibility tests**

Run:

```bash
mvn -Dtest=YunkaLoanApplyCompensationExecutorTest,QwBenefitPurchaseCompensationExecutorTest test
```

Expected:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

This proves consumer-side production code still parses the same stored JSON shape.

- [ ] **Step 2: Run the combined focused regression**

Run:

```bash
mvn -Dtest=AsyncCompensationEnqueueServiceTest,LoanApplicationServiceTest,BenefitOrderServiceTest,YunkaLoanApplyCompensationExecutorTest,QwBenefitPurchaseCompensationExecutorTest test
```

Expected:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 3: Run compile verification**

```bash
mvn -q -DskipTests compile
```

Expected:

- exit code `0`

- [ ] **Step 4: Confirm the two producer services no longer hand-build JSON payload strings**

Run:

```bash
rg -n '"""' \
  src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java
```

Expected:

- no matches
- exit code `1`

- [ ] **Step 5: Optionally verify persistence round-trip in MySQL when the environment is available**

Run only if the MySQL IT environment is already enabled:

```bash
MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlAsyncCompensationIntegrationTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 6: Do not create a commit if this task is verification-only**

Only create an extra commit if a small, plan-local fix is required to make Task 3 pass. If no code changes are needed, stop without a new commit.

---

## Suggested Commit Boundaries

1. `test: freeze async compensation typed payload behavior`
2. `refactor: add typed async compensation payloads`
3. no commit for verification-only Task 3

## Phase Exit Criteria

- `AsyncCompensationEnqueueService.EnqueueCommand` no longer accepts raw payload strings.
- `AsyncCompensationEnqueueServiceImpl` is the only production component responsible for payload JSON serialization.
- `LoanApplicationServiceImpl` no longer contains a text-block JSON payload for `YUNKA_LOAN_APPLY_RETRY`.
- `BenefitOrderServiceImpl` no longer contains a text-block JSON payload for `QW_BENEFIT_PURCHASE_RETRY`.
- Existing compensation executors still parse the stored JSON payload successfully without production-code changes.
- Focused unit tests and compile verification all pass.
