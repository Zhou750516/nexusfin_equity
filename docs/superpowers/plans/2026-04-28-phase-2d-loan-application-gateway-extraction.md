# Phase 2D Loan Application Gateway Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract a dedicated `LoanApplicationGateway` that centralizes loan application mapping persistence and lookup while keeping current loan apply/query business behavior unchanged.

**Architecture:** Introduce a small gateway service in front of `LoanApplicationMappingRepository` so mapping save and query details stop leaking into `LoanApplicationServiceImpl` and `LoanApprovalQueryServiceImpl`. The gateway owns entity construction, timestamp filling, repository query predicates, and active/pending mapping lookup, while both callers keep their current business decisions, response shaping, timeout compensation semantics, and not-found behavior.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/service/LoanApplicationGateway.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationGatewayImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationGatewayTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `docs/superpowers/plans/2026-04-28-phase-2a-loan-query-split.md`
- `docs/superpowers/plans/2026-04-28-phase-2c-loan-application-split.md`
- `src/main/java/com/nexusfin/equity/entity/LoanApplicationMapping.java`
- `src/main/java/com/nexusfin/equity/repository/LoanApplicationMappingRepository.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`

### Explicitly In Scope

- extract `LoanApplicationGateway`
- centralize `LoanApplicationMapping` save behavior used by loan apply success / timeout paths
- centralize `LoanApplicationMapping` active-or-pending lookup behavior used by loan approval query paths
- rewire `LoanApplicationServiceImpl` to use the gateway for mapping persistence
- rewire `LoanApprovalQueryServiceImpl` to use the gateway for mapping lookup

### Explicitly Out of Scope

- `AsyncCompensationEnqueueService` typed payload work
- moving `LoanApplyForwardData` or `LoanQueryForwardData` into new payload packages
- any `RepaymentServiceImpl` change
- any `YunkaLoanApplyCompensationExecutor` change
- any `XiaohuaGatewayServiceImpl` change
- any `.gitignore` change
- any new business behavior, protocol, or logging redesign

### Boundary Notes

- Keep `LoanService`, `LoanApplicationService`, and `LoanApprovalQueryService` interfaces unchanged.
- Keep controller entrypoints unchanged.
- Keep `LoanApplicationServiceImpl.apply(...)` business semantics unchanged:
  - rejected still returns `applicationId = null`, `status = "loan_failed"`
  - timeout still returns `status = "pending"` and enqueues async compensation
  - success still saves mapping with `ACTIVE`
- Keep `LoanApprovalQueryServiceImpl` business semantics unchanged:
  - missing mapping still throws `BizException(404, "application mapping not found")`
  - only `ACTIVE` and `PENDING_REVIEW` mappings participate in lookup
- The gateway should own repository predicates and entity construction, but it should not absorb caller-specific business responses.

### Validation Commands

- `mvn -Dtest=LoanApplicationGatewayTest test`
- `mvn -Dtest=LoanApplicationServiceTest,LoanApplicationGatewayTest test`
- `mvn -Dtest=LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test`
- `mvn -Dtest=LoanApplicationServiceTest,LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test`
- `mvn -q -DskipTests compile`
- `rg -n "LoanApplicationMappingRepository|Wrappers\\." src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`

---

### Task 1: Freeze Gateway Persistence and Lookup Behavior with Focused Tests

**Files:**
- Create: `src/test/java/com/nexusfin/equity/service/LoanApplicationGatewayTest.java`
- Test: `mvn -Dtest=LoanApplicationGatewayTest test`

- [ ] **Step 1: Write the failing gateway tests before introducing the gateway**

Create `src/test/java/com/nexusfin/equity/service/LoanApplicationGatewayTest.java` with this concrete test class:

```java
package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.impl.LoanApplicationGatewayImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationGatewayTest {

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    private LoanApplicationGateway loanApplicationGateway;

    @BeforeEach
    void setUp() {
        loanApplicationGateway = new LoanApplicationGatewayImpl(loanApplicationMappingRepository);
    }

    @Test
    void shouldSaveActiveMappingWithCurrentDefaults() {
        loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
                "mem-001",
                "user-001",
                "APP-001",
                "BEN-001",
                "LN-001",
                "rent",
                "ACTIVE"
        ));

        ArgumentCaptor<LoanApplicationMapping> captor = ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).insert(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo("APP-001");
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-001");
        assertThat(captor.getValue().getBenefitOrderNo()).isEqualTo("BEN-001");
        assertThat(captor.getValue().getChannelCode()).isEqualTo("KJ");
        assertThat(captor.getValue().getExternalUserId()).isEqualTo("user-001");
        assertThat(captor.getValue().getUpstreamQueryType()).isEqualTo("loanId");
        assertThat(captor.getValue().getUpstreamQueryValue()).isEqualTo("LN-001");
        assertThat(captor.getValue().getPurpose()).isEqualTo("rent");
        assertThat(captor.getValue().getMappingStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getCreatedTs()).isNotNull();
        assertThat(captor.getValue().getUpdatedTs()).isNotNull();
    }

    @Test
    void shouldSavePendingReviewMappingWithCurrentDefaults() {
        loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
                "mem-001",
                "user-001",
                "APP-002",
                "BEN-002",
                "LN-002",
                "education",
                "PENDING_REVIEW"
        ));

        ArgumentCaptor<LoanApplicationMapping> captor = ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).insert(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo("APP-002");
        assertThat(captor.getValue().getMappingStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(captor.getValue().getPurpose()).isEqualTo("education");
    }

    @Test
    void shouldFindActiveOrPendingMappingByMemberIdAndApplicationId() {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-003");
        mapping.setMemberId("mem-001");
        mapping.setUpstreamQueryValue("LN-003");
        mapping.setPurpose("rent");
        mapping.setMappingStatus("ACTIVE");
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(mapping);

        LoanApplicationMapping result =
                loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-003");

        assertThat(result).isSameAs(mapping);
        verify(loanApplicationMappingRepository).selectOne(any());
    }

    @Test
    void shouldReturnNullWhenActiveOrPendingMappingDoesNotExist() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        LoanApplicationMapping result =
                loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-404");

        assertThat(result).isNull();
        verify(loanApplicationMappingRepository).selectOne(any());
    }
}
```

- [ ] **Step 2: Run the focused test and confirm the red phase is only missing gateway types**

Run:

```bash
mvn -Dtest=LoanApplicationGatewayTest test
```

Expected red phase:

- `LoanApplicationGateway` does not exist
- `LoanApplicationGatewayImpl` does not exist

- [ ] **Step 3: Commit the red gateway tests**

```bash
git add src/test/java/com/nexusfin/equity/service/LoanApplicationGatewayTest.java
git commit -m "test: freeze loan application gateway behavior"
```

---

### Task 2: Implement LoanApplicationGateway with Repository Encapsulation

**Files:**
- Create: `src/main/java/com/nexusfin/equity/service/LoanApplicationGateway.java`
- Create: `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationGatewayImpl.java`
- Test: `mvn -Dtest=LoanApplicationGatewayTest test`

- [ ] **Step 1: Create the gateway interface with an explicit save command**

Create `src/main/java/com/nexusfin/equity/service/LoanApplicationGateway.java`:

```java
package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.LoanApplicationMapping;

public interface LoanApplicationGateway {

    void save(SaveCommand command);

    LoanApplicationMapping findActiveOrPendingMapping(String memberId, String applicationId);

    record SaveCommand(
            String memberId,
            String externalUserId,
            String applicationId,
            String benefitOrderNo,
            String upstreamLoanId,
            String purpose,
            String mappingStatus
    ) {
    }
}
```

- [ ] **Step 2: Implement the repository-backed gateway**

Create `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationGatewayImpl.java`:

```java
package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.LoanApplicationGateway;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class LoanApplicationGatewayImpl implements LoanApplicationGateway {

    private static final String DEFAULT_CHANNEL_CODE = "KJ";
    private static final String UPSTREAM_QUERY_TYPE = "loanId";

    private final LoanApplicationMappingRepository loanApplicationMappingRepository;

    public LoanApplicationGatewayImpl(LoanApplicationMappingRepository loanApplicationMappingRepository) {
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
    }

    @Override
    public void save(SaveCommand command) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(command.applicationId());
        mapping.setMemberId(command.memberId());
        mapping.setBenefitOrderNo(command.benefitOrderNo());
        mapping.setChannelCode(DEFAULT_CHANNEL_CODE);
        mapping.setExternalUserId(command.externalUserId());
        mapping.setUpstreamQueryType(UPSTREAM_QUERY_TYPE);
        mapping.setUpstreamQueryValue(command.upstreamLoanId());
        mapping.setPurpose(command.purpose());
        mapping.setMappingStatus(command.mappingStatus());
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        loanApplicationMappingRepository.insert(mapping);
    }

    @Override
    public LoanApplicationMapping findActiveOrPendingMapping(String memberId, String applicationId) {
        return loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getApplicationId, applicationId)
                        .eq(LoanApplicationMapping::getMemberId, memberId)
                        .in(LoanApplicationMapping::getMappingStatus, "ACTIVE", "PENDING_REVIEW")
                        .last("limit 1")
        );
    }
}
```

- [ ] **Step 3: Run the focused gateway tests until they pass**

Run:

```bash
mvn -Dtest=LoanApplicationGatewayTest test
```

Expected green phase:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 4: Commit the extracted gateway**

```bash
git add src/main/java/com/nexusfin/equity/service/LoanApplicationGateway.java \
        src/main/java/com/nexusfin/equity/service/impl/LoanApplicationGatewayImpl.java
git commit -m "refactor: extract loan application gateway"
```

---

### Task 3: Rewire LoanApplicationServiceImpl to Persist Through the Gateway

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- Test: `mvn -Dtest=LoanApplicationServiceTest,LoanApplicationGatewayTest test`

- [ ] **Step 1: Update the apply-service tests before changing production wiring**

Modify `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`:

- replace `@Mock LoanApplicationMappingRepository loanApplicationMappingRepository;` with `@Mock LoanApplicationGateway loanApplicationGateway;`
- construct `LoanApplicationServiceImpl` with `loanApplicationGateway`
- replace `verify(loanApplicationMappingRepository).insert(...)` assertions with `verify(loanApplicationGateway).save(...)`

Use these concrete assertion shapes:

```java
ArgumentCaptor<LoanApplicationGateway.SaveCommand> saveCaptor =
        ArgumentCaptor.forClass(LoanApplicationGateway.SaveCommand.class);
verify(loanApplicationGateway).save(saveCaptor.capture());
assertThat(saveCaptor.getValue().applicationId()).isEqualTo(response.applicationId());
assertThat(saveCaptor.getValue().benefitOrderNo()).isEqualTo("BEN-001");
assertThat(saveCaptor.getValue().upstreamLoanId()).isEqualTo("LN-UPSTREAM-001");
assertThat(saveCaptor.getValue().purpose()).isEqualTo("rent");
assertThat(saveCaptor.getValue().mappingStatus()).isEqualTo("ACTIVE");
```

And for the timeout path:

```java
ArgumentCaptor<LoanApplicationGateway.SaveCommand> saveCaptor =
        ArgumentCaptor.forClass(LoanApplicationGateway.SaveCommand.class);
verify(loanApplicationGateway).save(saveCaptor.capture());
assertThat(saveCaptor.getValue().applicationId()).isEqualTo(response.applicationId());
assertThat(saveCaptor.getValue().benefitOrderNo()).isEqualTo("BEN-TIMEOUT");
assertThat(saveCaptor.getValue().mappingStatus()).isEqualTo("PENDING_REVIEW");
assertThat(saveCaptor.getValue().upstreamLoanId()).startsWith("LN-");
```

- [ ] **Step 2: Run the focused tests and confirm the red phase is missing service rewiring only**

Run:

```bash
mvn -Dtest=LoanApplicationServiceTest,LoanApplicationGatewayTest test
```

Expected red phase:

- `LoanApplicationServiceImpl` constructor mismatch
- or old `LoanApplicationMappingRepository` interaction still being asserted

- [ ] **Step 3: Rewire LoanApplicationServiceImpl to depend on LoanApplicationGateway**

Update `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`:

- replace the `LoanApplicationMappingRepository` field with `LoanApplicationGateway`
- update constructor injection accordingly
- replace both mapping writes with gateway saves
- delete the local `saveApplicationMapping(...)` helper and the `DEFAULT_CHANNEL_CODE` constant
- keep all other apply-side behavior, request validation, timeout compensation, response semantics, and logging unchanged

The concrete replacement shape in `apply(...)` should be:

```java
loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
        memberId,
        uid,
        applicationId,
        benefitOrder.benefitOrderNo(),
        loanId,
        request.purpose(),
        "PENDING_REVIEW"
));
```

and:

```java
loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
        memberId,
        uid,
        applicationId,
        benefitOrder.benefitOrderNo(),
        upstreamLoanId,
        request.purpose(),
        "ACTIVE"
));
```

- [ ] **Step 4: Run the focused apply-service regression until it passes**

Run:

```bash
mvn -Dtest=LoanApplicationServiceTest,LoanApplicationGatewayTest test
```

Expected green phase:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 5: Commit the apply-service rewiring**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java
git commit -m "refactor: route loan application persistence through gateway"
```

---

### Task 4: Rewire LoanApprovalQueryServiceImpl to Query Through the Gateway

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`
- Test: `mvn -Dtest=LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test`

- [ ] **Step 1: Update the query-service tests before changing production wiring**

Modify `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`:

- replace `@Mock LoanApplicationMappingRepository loanApplicationMappingRepository;` with `@Mock LoanApplicationGateway loanApplicationGateway;`
- construct `LoanApprovalQueryServiceImpl` with `loanApplicationGateway`
- replace repository stubs with gateway stubs

Use these concrete changes:

```java
when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-003"))
        .thenReturn(mapping("APP-003", "LN-003", "rent"));
```

```java
when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-001"))
        .thenReturn(mapping("APP-001", "LN-001", "rent"));
```

```java
when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-404"))
        .thenReturn(null);
```

- [ ] **Step 2: Run the focused tests and confirm the red phase is missing service rewiring only**

Run:

```bash
mvn -Dtest=LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test
```

Expected red phase:

- `LoanApprovalQueryServiceImpl` constructor mismatch
- or old repository stubs are no longer used

- [ ] **Step 3: Rewire LoanApprovalQueryServiceImpl to depend on LoanApplicationGateway**

Update `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`:

- replace `LoanApplicationMappingRepository` with `LoanApplicationGateway`
- remove the `Wrappers` import from this service
- change `findMapping(...)` to call the gateway, then keep the current `BizException(404, "application mapping not found")` behavior when the gateway returns `null`
- keep all Yunka query, repay-plan fallback, approval status mapping, and i18n behavior unchanged

The `findMapping(...)` method should become:

```java
private LoanApplicationMapping findMapping(String memberId, String applicationId) {
    LoanApplicationMapping mapping =
            loanApplicationGateway.findActiveOrPendingMapping(memberId, applicationId);
    if (mapping == null) {
        throw new BizException(404, "application mapping not found");
    }
    return mapping;
}
```

- [ ] **Step 4: Run the focused query-service regression until it passes**

Run:

```bash
mvn -Dtest=LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test
```

Expected green phase:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 5: Commit the query-service rewiring**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java
git commit -m "refactor: route loan approval mapping queries through gateway"
```

---

### Task 5: Final Focused Verification and Structure Check

**Files:**
- Verify only, no intended code change
- Test: `mvn -Dtest=LoanApplicationServiceTest,LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test`
- Test: `mvn -q -DskipTests compile`

- [ ] **Step 1: Run the focused regression suite**

Run:

```bash
mvn -Dtest=LoanApplicationServiceTest,LoanApprovalQueryServiceTest,LoanApplicationGatewayTest test
```

Expected:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 2: Run compile verification**

Run:

```bash
mvn -q -DskipTests compile
```

Expected:

- exit code `0`

- [ ] **Step 3: Confirm the two services no longer reach for repository details directly**

Run:

```bash
rg -n "LoanApplicationMappingRepository|Wrappers\\." \
  src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java
```

Expected:

- no matches

- [ ] **Step 4: Do not create a commit if this task is verification-only**

Only create an extra commit if a small, plan-local fix is required to make Task 5 pass. If no code changes are needed, stop without a new commit.

---

## Suggested Commit Boundaries

1. `test: freeze loan application gateway behavior`
2. `refactor: extract loan application gateway`
3. `refactor: route loan application persistence through gateway`
4. `refactor: route loan approval mapping queries through gateway`
5. no commit for verification-only Task 5

## Phase Exit Criteria

- `LoanApplicationGateway` exists and is the only component that directly builds `LoanApplicationMapping` entities for apply/query flows.
- `LoanApplicationServiceImpl` no longer imports or injects `LoanApplicationMappingRepository`.
- `LoanApprovalQueryServiceImpl` no longer imports `LoanApplicationMappingRepository` or `Wrappers`.
- `LoanApplicationServiceTest`, `LoanApprovalQueryServiceTest`, and `LoanApplicationGatewayTest` all pass together.
- No behavior change is introduced in apply success / rejected / timeout flows or approval-query not-found behavior.
