# Phase 2A Loan Query Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the loan query-side responsibilities out of `LoanServiceImpl` into a dedicated service while keeping the `LoanService` interface, `LoanController` entrypoints, and all current query behavior unchanged.

**Architecture:** Introduce a dedicated `LoanApprovalQueryService` inside the existing service layer. `LoanServiceImpl` remains the facade that implements `LoanService`, but its `getApprovalStatus(...)` and `getApprovalResult(...)` methods become thin delegators. The new query service owns mapping lookup, Yunka loan query, Xiaohua repay-plan query, and approval/result DTO mapping. Apply-side and calculate-side logic stay in `LoanServiceImpl`, and no payload records are moved to a new package in this phase.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/service/LoanApprovalQueryService.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `docs/superpowers/plans/2026-04-28-phase-1b-yunka-call-template.md`
- `src/main/java/com/nexusfin/equity/service/LoanService.java`
- `src/main/java/com/nexusfin/equity/controller/LoanController.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/XiaohuaGatewayService.java`
- `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

### Explicitly In Scope

- `LoanServiceImpl.getApprovalStatus(...)`
- `LoanServiceImpl.getApprovalResult(...)`
- query-only helper logic directly serving the two methods above
- `queryLoan(...)`
- `queryRepayPlan(...)`
- facade delegation from `LoanServiceImpl` to the new query service

### Explicitly Out of Scope

- `LoanServiceImpl.apply(...)`
- `LoanServiceImpl.calculate(...)`
- any `RepaymentServiceImpl` split
- moving `LoanApplyForwardData`, `LoanQueryForwardData`, or `Repay*ForwardData` into a shared payload package
- `AsyncCompensationEnqueueService` typed payload work
- any new changes in `XiaohuaGatewayServiceImpl`
- any new changes in `YunkaLoanApplyCompensationExecutor`
- any Phase 3 `thirdparty/qw` consolidation

### Boundary Notes

- Keep `LoanService` unchanged.
- Keep `LoanController` unchanged.
- `LoanServiceImpl` may gain one new dependency, `LoanApprovalQueryService`, and delegate query calls to it.
- Do not widen this phase into “shared helper cleanup” if a helper is still used by apply-side code.
- If a tiny helper is needed on both sides but only the query side is being moved, prefer duplicating the helper locally in `LoanApprovalQueryServiceImpl` over extracting a broader shared utility in Phase 2A.

### Validation Commands

- `mvn -Dtest=LoanApprovalQueryServiceTest test`
- `mvn -Dtest=LoanServiceTest,LoanApprovalQueryServiceTest test`
- `mvn -q -DskipTests compile`
- `rg -n "private LoanApplicationMapping findMapping|private JsonNode queryLoan|private List<LoanApprovalResultResponse.RepaymentPlanItem> queryRepayPlan|private String mapApprovalStatus|private List<LoanApprovalStatusResponse.ApprovalStep> buildApprovalStatusSteps|private LoanApprovalStatusResponse.BenefitsCardPreview buildBenefitsCardPreview" src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`

---

### Task 1: Freeze Loan Query Behavior with Focused Tests

**Files:**
- Create: `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`
- Test: `mvn -Dtest=LoanApprovalQueryServiceTest test`

- [ ] **Step 1: Write the failing query-service tests before introducing the service**

Create `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java` with this concrete test class:

```java
package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.impl.LoanApprovalQueryServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanItem;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApprovalQueryServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private YunkaCallTemplate yunkaCallTemplate;

    private LoanApprovalQueryService loanApprovalQueryService;

    @BeforeEach
    void setUp() {
        when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        loanApprovalQueryService = new LoanApprovalQueryServiceImpl(
                h5BenefitsProperties(),
                yunkaProperties(),
                loanApplicationMappingRepository,
                h5I18nService,
                xiaohuaGatewayService,
                yunkaCallTemplate
        );
    }

    @Test
    void shouldBuildRejectedApprovalStatusWithBenefitsPreview() {
        when(loanApplicationMappingRepository.selectOne(any()))
                .thenReturn(mapping("APP-003", "LN-003", "rent"));
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.createObjectNode()
                        .put("status", "7003")
                        .put("remark", "invalid state"));

        LoanApprovalStatusResponse response = loanApprovalQueryService.getApprovalStatus("mem-001", "APP-003");

        assertThat(response.applicationId()).isEqualTo("APP-003");
        assertThat(response.status()).isEqualTo("rejected");
        assertThat(response.purpose()).isEqualTo("rent");
        assertThat(response.benefitsCard().available()).isTrue();
        assertThat(response.benefitsCard().price()).isEqualTo(300L);
        assertThat(response.benefitsCard().features()).containsExactly("免息券", "会员折扣", "专属活动");
        assertThat(response.steps())
                .extracting(LoanApprovalStatusResponse.ApprovalStep::status)
                .containsExactly("completed", "completed", "pending");
    }

    @Test
    void shouldBuildApprovedApprovalResultAndMapRepayPlan() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any()))
                .thenReturn(mapping("APP-001", "LN-001", "rent"));
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "status": "7001",
                          "loanAmount": 300000,
                          "remark": "放款成功"
                        }
                        """));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("APP-001"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, "2026-06-07", 100000L, 3000L, 103000L)
                )));

        LoanApprovalResultResponse response = loanApprovalQueryService.getApprovalResult("mem-001", "APP-001");

        assertThat(response.applicationId()).isEqualTo("APP-001");
        assertThat(response.status()).isEqualTo("approved");
        assertThat(response.purpose()).isEqualTo("rent");
        assertThat(response.approvedAmount()).isEqualByComparingTo("3000.00");
        assertThat(response.tip()).isEqualTo("审批通过，预计30分钟内到账");
        assertThat(response.loanId()).isEqualTo("LN-001");
        assertThat(response.repaymentPlan()).hasSize(2);
        assertThat(response.repaymentPlan().get(0).repaymentAmount()).isEqualByComparingTo("1045.00");
    }

    @Test
    void shouldReturnEmptyRepayPlanWhenRepayPlanQueryThrowsBizException() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any()))
                .thenReturn(mapping("APP-002", "LN-002", "education"));
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "status": "7001",
                          "loanAmount": 280000,
                          "remark": "审批通过，预计30分钟内到账"
                        }
                        """));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("APP-002"), any()))
                .thenThrow(new BizException("YUNKA_UPSTREAM_REJECTED", "repay plan unavailable"));

        LoanApprovalResultResponse response = loanApprovalQueryService.getApprovalResult("mem-001", "APP-002");

        assertThat(response.status()).isEqualTo("approved");
        assertThat(response.loanId()).isEqualTo("LN-002");
        assertThat(response.repaymentPlan()).isEmpty();
    }

    @Test
    void shouldThrowNotFoundWhenApplicationMappingDoesNotExist() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> loanApprovalQueryService.getApprovalStatus("mem-001", "APP-404"))
                .isInstanceOf(BizException.class)
                .extracting(
                        throwable -> ((BizException) throwable).getCode(),
                        throwable -> ((BizException) throwable).getErrorMsg()
                )
                .containsExactly(404, "application mapping not found");
    }

    private LoanApplicationMapping mapping(String applicationId, String loanId, String purpose) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(applicationId);
        mapping.setMemberId("mem-001");
        mapping.setBenefitOrderNo("BEN-001");
        mapping.setChannelCode("KJ");
        mapping.setExternalUserId("user-001");
        mapping.setUpstreamQueryType("loanId");
        mapping.setUpstreamQueryValue(loanId);
        mapping.setPurpose(purpose);
        mapping.setMappingStatus("ACTIVE");
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        return mapping;
    }

    private H5BenefitsProperties h5BenefitsProperties() {
        return new H5BenefitsProperties(
                "HUXUAN_CARD",
                new H5BenefitsProperties.Activate(30000L, "huixuan_card", "惠选卡开通成功"),
                new H5BenefitsProperties.Detail(
                        "惠选卡",
                        300L,
                        448L,
                        List.of(
                                new H5BenefitsProperties.Feature("免息券", "f1"),
                                new H5BenefitsProperties.Feature("会员折扣", "f2"),
                                new H5BenefitsProperties.Feature("专属活动", "f3"),
                                new H5BenefitsProperties.Feature("备用权益", "f4")
                        ),
                        List.of(),
                        List.of("tip"),
                        List.of()
                )
        );
    }

    private YunkaProperties yunkaProperties() {
        return new YunkaProperties(
                true,
                "REST",
                "http://localhost:8080",
                "/api/gateway/proxy",
                3000,
                5000,
                new YunkaProperties.Paths(
                        "/loan/trail",
                        "/loan/query",
                        "/loan/apply",
                        "/repay/trial",
                        "/repay/apply",
                        "/repay/query",
                        "/protocol/queryProtocolAggregationLink",
                        "/user/token",
                        "/user/query",
                        "/loan/repayPlan",
                        "/card/smsSend",
                        "/card/smsConfirm",
                        "/card/userCards",
                        "/credit/image/query",
                        "/benefit/sync"
                )
        );
    }
}
```

- [ ] **Step 2: Run the new test and confirm it fails because the service does not exist yet**

Run:

```bash
mvn -Dtest=LoanApprovalQueryServiceTest test
```

Expected: compilation fails with `cannot find symbol` for `LoanApprovalQueryService` / `LoanApprovalQueryServiceImpl`.

- [ ] **Step 3: Commit only the failing-test checkpoint if the session policy requires it**

No commit is required at this point unless the execution session explicitly asks for a red-phase checkpoint. Default behavior for this repo is to continue to the minimal implementation and commit a green task boundary.

### Task 2: Implement `LoanApprovalQueryService` and Move Query-Side Behavior

**Files:**
- Create: `src/main/java/com/nexusfin/equity/service/LoanApprovalQueryService.java`
- Create: `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`
- Create: `src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java`
- Test: `mvn -Dtest=LoanApprovalQueryServiceTest test`

- [ ] **Step 1: Add the new query service interface**

Create `LoanApprovalQueryService.java`:

```java
public interface LoanApprovalQueryService {

    LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId);

    LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId);
}
```

- [ ] **Step 2: Create the concrete query service shell with explicit constructor dependencies**

Create `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java` with this class shell and these exact dependencies:

```java
package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanApprovalQueryService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanRequest;
import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;
import com.nexusfin.equity.util.TraceIdUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoanApprovalQueryServiceImpl implements LoanApprovalQueryService {

    private static final Logger log = LoggerFactory.getLogger(LoanApprovalQueryServiceImpl.class);
    private static final String LOAN_STATUS_SUCCESS = "7001";
    private static final String LOAN_STATUS_PROCESSING = "7002";
    private static final String LOAN_STATUS_FAILURE = "7003";

    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final H5I18nService h5I18nService;
    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanApprovalQueryServiceImpl(
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            H5I18nService h5I18nService,
            XiaohuaGatewayService xiaohuaGatewayService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.h5I18nService = h5I18nService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    public LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId) {
        LoanApplicationMapping mapping = findMapping(memberId, applicationId);
        JsonNode data = queryLoan(mapping);
        String h5Status = mapApprovalStatus(data.path("status").asText());
        return new LoanApprovalStatusResponse(
                applicationId,
                h5Status,
                mapping.getPurpose(),
                buildApprovalStatusSteps(h5Status),
                buildBenefitsCardPreview()
        );
    }

    @Override
    public LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId) {
        LoanApplicationMapping mapping = findMapping(memberId, applicationId);
        JsonNode data = queryLoan(mapping);
        String h5Status = mapApprovalStatus(data.path("status").asText());
        boolean approved = "approved".equals(h5Status);
        boolean reviewing = "reviewing".equals(h5Status);
        return new LoanApprovalResultResponse(
                applicationId,
                h5Status,
                mapping.getPurpose(),
                approved ? centsToYuan(data.path("loanAmount").asLong(0L)) : java.math.BigDecimal.ZERO,
                approved || reviewing ? h5I18nService.text("loan.approval.arrivalTime", "30分钟") : "--",
                buildApprovalStatusSteps(h5Status),
                true,
                resolveApprovalResultTip(data, h5Status),
                approved ? mapping.getUpstreamQueryValue() : null,
                approved ? queryRepayPlan(mapping) : List.of()
        );
    }
}
```

- [ ] **Step 3: Move the query-owned helpers and keep the apply-owned helpers in `LoanServiceImpl`**

Move these methods out of `LoanServiceImpl` into `LoanApprovalQueryServiceImpl`:

- Move unchanged:
  - `findMapping(...)`
  - `buildBenefitsCardPreview(...)`
  - `buildApprovalStatusSteps(...)`
  - `buildApprovalResultSteps(...)`
  - `mapApprovalStatus(...)`
  - `defaultLong(...)`
  - `isGenericApprovedRemark(...)`
- Move with local adaptation inside the new service:
  - `queryLoan(...)`
    - keep a private `LoanQueryForwardData` record inside `LoanApprovalQueryServiceImpl`
    - keep request id prefix `LQ`
    - keep `bizOrderNo = mapping.getApplicationId()`
  - `resolveApprovalResultTip(...)`
    - add a local private `readText(JsonNode data, String fieldName, String fallback)` helper in the new service
  - `queryRepayPlan(...)`
    - keep the current `BizException` catch-and-return-empty-list behavior unchanged
    - keep the same warning log fields, only the logger class changes

Keep these helpers in `LoanServiceImpl` because they still belong to calculate/apply:

- `validateCalculateRequest(...)`
- `validateApplyRequest(...)`
- `validateAmountAndTerm(...)`
- `buildLoanFailedResponse(...)`
- `saveApplicationMapping(...)`
- `readAnnualRate(...)`
- `readRemark(JsonNode data)`
- `readRemark(JsonNode data, String fallback)`
- `readText(JsonNode data, String fieldName, String fallback)`
- `resolveBankCardNum(...)`
- `resolvePlatformBenefitOrderNo(...)`
- `hasText(...)`
- `LoanTrailForwardData`
- `LoanApplyForwardData`

Use this exact local record in `LoanApprovalQueryServiceImpl` instead of moving payloads to a new package:

```java
private record LoanQueryForwardData(
        String uid,
        String loanId
) {
}
```

Preserve these logic points exactly while moving the helpers:

- current status mapping:

```java
return switch (upstreamStatus) {
    case "7001" -> "approved";
    case "7003", "7004", "7008", "7009" -> "rejected";
    case "7002" -> "reviewing";
    default -> "reviewing";
};
```

- current repay-plan fallback:

```java
catch (BizException exception) {
    log.warn("traceId={} bizOrderNo={} requestId={} loan repay plan query failed errorNo={} errorMsg={}",
            TraceIdUtil.getTraceId(),
            mapping.getApplicationId(),
            requestId,
            exception.getErrorNo(),
            exception.getErrorMsg());
    return List.of();
}
```

- Preserve the current “generic approved remark falls back to i18n approved tip” rule.
- Do not move apply-side `readRemark(...)`, `readText(...)`, `resolveBankCardNum(...)`, `resolvePlatformBenefitOrderNo(...)`, or any apply payload record in this phase.

- [ ] **Step 4: Run the focused query-service test and make it pass**

Run:

```bash
mvn -Dtest=LoanApprovalQueryServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit the extracted query service**

Suggested commit boundary:

```bash
git add src/main/java/com/nexusfin/equity/service/LoanApprovalQueryService.java \
        src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanApprovalQueryServiceTest.java
git commit -m "refactor: extract loan approval query service"
```

### Task 3: Turn `LoanServiceImpl` into a Query Facade for the Existing API

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- Test: `mvn -Dtest=LoanServiceTest,LoanApprovalQueryServiceTest test`

- [ ] **Step 1: Replace the two query entrypoints with delegation**

Update `LoanServiceImpl` constructor to inject `LoanApprovalQueryService`, then reduce the two public query methods to:

```java
@Override
public LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId) {
    return loanApprovalQueryService.getApprovalStatus(memberId, applicationId);
}

@Override
public LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId) {
    return loanApprovalQueryService.getApprovalResult(memberId, applicationId);
}
```

- [ ] **Step 2: Update `LoanServiceTest` setup to inject a `LoanApprovalQueryService` mock**

Add a new mock field and wire it into the constructor so `LoanServiceTest` continues to cover apply-side behavior while gaining facade-level delegation assertions:

```java
@Mock
private LoanApprovalQueryService loanApprovalQueryService;

@BeforeEach
void setUp() {
    when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
    loanService = new LoanServiceImpl(
            h5LoanProperties(),
            h5BenefitsProperties(),
            yunkaProperties(),
            yunkaGatewayClient,
            loanApplicationMappingRepository,
            benefitOrderService,
            h5I18nService,
            asyncCompensationEnqueueService,
            xiaohuaGatewayService,
            new YunkaCallTemplate(yunkaGatewayClient),
            loanApprovalQueryService
    );
}
```

- [ ] **Step 3: Remove only the helpers that are now owned by the new service**

Delete from `LoanServiceImpl` once delegation is in place:

- `findMapping(...)`
- `queryLoan(...)`
- `buildBenefitsCardPreview(...)`
- `buildApprovalStatusSteps(...)`
- `buildApprovalResultSteps(...)`
- `mapApprovalStatus(...)`
- `resolveApprovalResultTip(...)`
- `queryRepayPlan(...)`
- `defaultLong(...)`
- `isGenericApprovedRemark(...)`
- query-side `LoanQueryForwardData`

Leave in `LoanServiceImpl`:

- `calculate(...)` and all calculator helpers
- `apply(...)` and all apply helpers
- apply-side `readText(...)` / `readRemark(...)` overloads
- `LoanTrailForwardData`
- `LoanApplyForwardData`

- [ ] **Step 4: Add facade-level delegation coverage in `LoanServiceTest`**

Add these concrete tests to `LoanServiceTest`:

```java
@Test
void shouldDelegateApprovalStatusToLoanApprovalQueryService() {
    LoanApprovalStatusResponse delegated = new LoanApprovalStatusResponse(
            "APP-STATUS-001",
            "reviewing",
            "rent",
            List.of(),
            new LoanApprovalStatusResponse.BenefitsCardPreview(true, 300L, List.of("免息券"))
    );
    when(loanApprovalQueryService.getApprovalStatus("mem-001", "APP-STATUS-001")).thenReturn(delegated);

    LoanApprovalStatusResponse response = loanService.getApprovalStatus("mem-001", "APP-STATUS-001");

    assertThat(response).isSameAs(delegated);
    verify(loanApprovalQueryService).getApprovalStatus("mem-001", "APP-STATUS-001");
}

@Test
void shouldDelegateApprovalResultToLoanApprovalQueryService() {
    LoanApprovalResultResponse delegated = new LoanApprovalResultResponse(
            "APP-RESULT-001",
            "approved",
            "rent",
            new java.math.BigDecimal("3000.00"),
            "30分钟",
            List.of(),
            true,
            "审批通过，预计30分钟内到账",
            "LN-001",
            List.of()
    );
    when(loanApprovalQueryService.getApprovalResult("mem-001", "APP-RESULT-001")).thenReturn(delegated);

    LoanApprovalResultResponse response = loanService.getApprovalResult("mem-001", "APP-RESULT-001");

    assertThat(response).isSameAs(delegated);
    verify(loanApprovalQueryService).getApprovalResult("mem-001", "APP-RESULT-001");
}
```

Keep all existing apply-side tests in `LoanServiceTest` intact. Do not repurpose this task into a broader `LoanServiceImpl` test rewrite.

- [ ] **Step 5: Run the focused service tests**

Run:

```bash
mvn -Dtest=LoanServiceTest,LoanApprovalQueryServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit the facade wiring**

Suggested commit boundary:

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanServiceTest.java
git commit -m "refactor: delegate loan query facade to extracted service"
```

### Task 4: Final Focused Verification and Structure Check

**Files:**
- Modify: none expected
- Test: `mvn -Dtest=LoanServiceTest,LoanApprovalQueryServiceTest test`

- [ ] **Step 1: Run the focused regression suite**

Run:

```bash
mvn -Dtest=LoanServiceTest,LoanApprovalQueryServiceTest test
```

Expected: PASS.

- [ ] **Step 2: Run compile verification**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: PASS.

- [ ] **Step 3: Run a structure grep to prove query helpers left `LoanServiceImpl`**

Run:

```bash
rg -n "private LoanApplicationMapping findMapping|private JsonNode queryLoan|private List<LoanApprovalResultResponse.RepaymentPlanItem> queryRepayPlan|private String mapApprovalStatus|private List<LoanApprovalStatusResponse.ApprovalStep> buildApprovalStatusSteps|private LoanApprovalStatusResponse.BenefitsCardPreview buildBenefitsCardPreview" \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java
```

Expected: no matches.

- [ ] **Step 4: Do not create an empty commit**

If Task 4 is verification-only and no code changed, stop without creating a commit.

---

## Suggested Commit Plan

1. `refactor: extract loan approval query service`
2. `refactor: delegate loan query facade to extracted service`
3. No commit for final verification if no code changes are needed

## Self-Review

- Scope stays inside `LoanServiceImpl` query side only.
- `LoanService` and `LoanController` remain unchanged.
- No Phase 2B apply-side work is included.
- No `RepaymentServiceImpl` or `thirdparty/qw` work is mixed in.
- Shared helper extraction is intentionally deferred unless it is required to finish the query split without changing behavior.
