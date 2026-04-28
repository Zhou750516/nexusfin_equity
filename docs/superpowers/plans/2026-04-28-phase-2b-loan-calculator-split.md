# Phase 2B Loan Calculator Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the calculator-side responsibilities out of `LoanServiceImpl` into a dedicated service while keeping the `LoanService` interface, `LoanController` entrypoints, request/response shapes, and current calculate behavior unchanged.

**Architecture:** Introduce a dedicated `LoanCalculatorService` inside the existing service layer. `LoanServiceImpl` remains the facade that implements `LoanService`, but its `getCalculatorConfig()` and `calculate(...)` methods become thin delegators. The new calculator service owns config assembly, calculate request validation, Yunka `/loan/trail` invocation, and repayment-plan / annual-rate mapping. Apply-side validation and all apply/query responsibilities stay where they are.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/service/LoanCalculatorService.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanCalculatorServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `docs/superpowers/plans/2026-04-28-phase-2a-loan-query-split.md`
- `src/main/java/com/nexusfin/equity/service/LoanService.java`
- `src/main/java/com/nexusfin/equity/controller/LoanController.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

### Explicitly In Scope

- `LoanServiceImpl.getCalculatorConfig()`
- `LoanServiceImpl.calculate(...)`
- calculator-only helper logic directly serving the two methods above
- Yunka `/loan/trail` forwarding data used only by `calculate(...)`
- facade delegation from `LoanServiceImpl` to the new calculator service

### Explicitly Out of Scope

- `LoanServiceImpl.apply(...)`
- `LoanServiceImpl.validateApplyRequest(...)`
- `LoanServiceImpl.saveApplicationMapping(...)`
- any `LoanApprovalQueryService` behavior
- any `RepaymentServiceImpl` split
- moving payload `record` classes into `thirdparty/yunka/payload/`
- `AsyncCompensationEnqueueService` typed payload work
- any new changes in `XiaohuaGatewayServiceImpl`
- any new changes in `YunkaLoanApplyCompensationExecutor`
- any Phase 3 `thirdparty/qw` consolidation

### Boundary Notes

- Keep `LoanService` unchanged.
- Keep `LoanController` unchanged.
- `LoanServiceImpl` may gain one new dependency, `LoanCalculatorService`, and delegate calculator calls to it.
- Keep apply-side amount/term validation semantics unchanged. Because `validateAmountAndTerm(...)` is still used by apply-side code, do not move it out of `LoanServiceImpl` in this phase unless you deliberately duplicate the narrower calculator validation inside `LoanCalculatorServiceImpl`.
- Do not opportunistically delete the unused `XiaohuaGatewayService` constructor parameter in this phase unless it becomes necessary to compile. That cleanup belongs to a separate, explicit task.

### Validation Commands

- `mvn -Dtest=LoanCalculatorServiceTest test`
- `mvn -Dtest=LoanServiceTest,LoanCalculatorServiceTest test`
- `mvn -q -DskipTests compile`
- `rg -n "private List<LoanCalculatorConfigResponse.TermOption> mapTermOptions|private void validateCalculateRequest|private List<LoanCalculateResponse.RepaymentPlanItem> mapRepaymentPlan|private String readAnnualRate|private record LoanTrailForwardData" src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`

---

### Task 1: Freeze Calculator Behavior with Focused Query-Free Tests

**Files:**
- Create: `src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java`
- Test: `mvn -Dtest=LoanCalculatorServiceTest test`

- [ ] **Step 1: Write the failing calculator-service tests before introducing the service**

Create `src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java` with this concrete test class:

```java
package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.impl.LoanCalculatorServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanCalculatorServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private YunkaCallTemplate yunkaCallTemplate;

    private LoanCalculatorService loanCalculatorService;

    @BeforeEach
    void setUp() {
        when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        loanCalculatorService = new LoanCalculatorServiceImpl(
                h5LoanProperties(),
                yunkaProperties(),
                h5I18nService,
                yunkaCallTemplate
        );
    }

    @Test
    void shouldBuildCalculatorConfigFromPropertiesAndI18nDefaults() {
        LoanCalculatorConfigResponse response = loanCalculatorService.getCalculatorConfig();

        assertThat(response.amountRange().min()).isEqualTo(100L);
        assertThat(response.amountRange().defaultAmount()).isEqualTo(3000L);
        assertThat(response.termOptions())
                .extracting(LoanCalculatorConfigResponse.TermOption::value)
                .containsExactly(3, 6);
        assertThat(response.annualRate()).isEqualByComparingTo("0.18");
        assertThat(response.lender()).isEqualTo("XX商业银行");
        assertThat(response.receivingAccount().bankName()).isEqualTo("招商银行");
        assertThat(response.receivingAccount().lastFour()).isEqualTo("8648");
        assertThat(response.receivingAccount().accountId()).isEqualTo("acc_001");
    }

    @Test
    void shouldCalculateRepaymentPlanFromYunkaTrailResponse() throws Exception {
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "receiveAmount": 300000,
                          "repayAmount": 312345,
                          "yearRate": 18.0,
                          "repayPlan": [
                            {
                              "period": 1,
                              "date": "2026-05-07",
                              "principal": 100000,
                              "interest": 4500,
                              "total": 104500
                            },
                            {
                              "period": 2,
                              "date": "2026-06-07",
                              "principal": 100000,
                              "interest": 4000,
                              "total": 104000
                            }
                          ]
                        }
                        """));

        LoanCalculateResponse response = loanCalculatorService.calculate(
                "mem-001",
                "user-001",
                new LoanCalculateRequest(3000L, 3)
        );

        assertThat(response.totalFee()).isEqualByComparingTo("123.45");
        assertThat(response.annualRate()).isEqualTo("18.0%");
        assertThat(response.repaymentPlan()).hasSize(2);
        assertThat(response.repaymentPlan().get(0).total()).isEqualByComparingTo("1045.00");

        ArgumentCaptor<YunkaCallTemplate.YunkaCall> captor = ArgumentCaptor.forClass(YunkaCallTemplate.YunkaCall.class);
        verify(yunkaCallTemplate).executeForData(captor.capture());
        assertThat(captor.getValue().scene()).isEqualTo("loan calculate");
        assertThat(captor.getValue().memberId()).isEqualTo("mem-001");
        assertThat(captor.getValue().path()).isEqualTo("/loan/trail");
    }

    @Test
    void shouldFallbackToConfigAnnualRateAndRequestedAmountWhenYunkaFieldsAreMissing() throws Exception {
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "repayPlan": []
                        }
                        """));

        LoanCalculateResponse response = loanCalculatorService.calculate(
                "mem-001",
                "user-001",
                new LoanCalculateRequest(3000L, 3)
        );

        assertThat(response.totalFee()).isEqualByComparingTo("0.00");
        assertThat(response.annualRate()).isEqualTo("18.0%");
        assertThat(response.repaymentPlan()).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedAmountOrTermBeforeCallingYunka() {
        assertThatThrownBy(() -> loanCalculatorService.calculate(
                "mem-001",
                "user-001",
                new LoanCalculateRequest(3050L, 3)
        ))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("amount step is invalid");

        assertThatThrownBy(() -> loanCalculatorService.calculate(
                "mem-001",
                "user-001",
                new LoanCalculateRequest(3000L, 12)
        ))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("term is unsupported");
    }

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(
                        new H5LoanProperties.TermOption("3期", 3),
                        new H5LoanProperties.TermOption("6期", 6)
                ),
                BigDecimal.valueOf(0.18),
                "XX商业银行",
                new H5LoanProperties.ReceivingAccount("招商银行", "8648", "acc_001")
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

- [ ] **Step 2: Run the focused test and confirm it fails because the new service does not exist yet**

Run:

```bash
mvn -Dtest=LoanCalculatorServiceTest test
```

Expected failure shape:

- `cannot find symbol`
- `LoanCalculatorService`
- `LoanCalculatorServiceImpl`

- [ ] **Step 3: Commit the red test only**

```bash
git add src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java
git commit -m "test: freeze loan calculator service behavior"
```

### Task 2: Introduce the Calculator Service and Move Calculator-Only Logic

**Files:**
- Create: `src/main/java/com/nexusfin/equity/service/LoanCalculatorService.java`
- Create: `src/main/java/com/nexusfin/equity/service/impl/LoanCalculatorServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java`
- Test: `mvn -Dtest=LoanCalculatorServiceTest test`

- [ ] **Step 1: Add the service interface**

Create `src/main/java/com/nexusfin/equity/service/LoanCalculatorService.java`:

```java
package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;

public interface LoanCalculatorService {

    LoanCalculatorConfigResponse getCalculatorConfig();

    LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request);
}
```

- [ ] **Step 2: Implement the calculator service with only calculator-side dependencies**

Create `src/main/java/com/nexusfin/equity/service/impl/LoanCalculatorServiceImpl.java` with this structure:

```java
package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanCalculatorService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;

@Service
public class LoanCalculatorServiceImpl implements LoanCalculatorService {

    private final H5LoanProperties h5LoanProperties;
    private final YunkaProperties yunkaProperties;
    private final H5I18nService h5I18nService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanCalculatorServiceImpl(
            H5LoanProperties h5LoanProperties,
            YunkaProperties yunkaProperties,
            H5I18nService h5I18nService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.yunkaProperties = yunkaProperties;
        this.h5I18nService = h5I18nService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    public LoanCalculatorConfigResponse getCalculatorConfig() {
        H5LoanProperties.ReceivingAccount receivingAccount = h5LoanProperties.receivingAccount();
        return new LoanCalculatorConfigResponse(
                new LoanCalculatorConfigResponse.AmountRange(
                        h5LoanProperties.amountRange().min(),
                        h5LoanProperties.amountRange().max(),
                        h5LoanProperties.amountRange().step(),
                        h5LoanProperties.amountRange().defaultAmount()
                ),
                mapTermOptions(h5LoanProperties.termOptions()),
                h5LoanProperties.annualRate(),
                h5I18nService.text("loan.lender", h5LoanProperties.lender()),
                new LoanCalculatorConfigResponse.ReceivingAccount(
                        h5I18nService.text("loan.receivingAccount.bankName", receivingAccount.bankName()),
                        receivingAccount.lastFour(),
                        receivingAccount.accountId()
                )
        );
    }

    @Override
    public LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request) {
        validateCalculateRequest(request);
        String requestId = next("LC");
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan calculate",
                        requestId,
                        yunkaProperties.paths().loanCalculate(),
                        requestId,
                        new LoanTrailForwardData(uid, requestId, yuanToCent(request.amount()), request.term())
                ).withMemberId(memberId)
        );
        long receiveAmount = data.path("receiveAmount").asLong(yuanToCent(request.amount()));
        long repayAmount = data.path("repayAmount").asLong(receiveAmount);
        return new LoanCalculateResponse(
                centsToYuan(repayAmount - receiveAmount),
                readAnnualRate(data),
                mapRepaymentPlan(data.path("repayPlan"))
        );
    }

    private List<LoanCalculatorConfigResponse.TermOption> mapTermOptions(List<H5LoanProperties.TermOption> termOptions) {
        return termOptions.stream()
                .map(termOption -> new LoanCalculatorConfigResponse.TermOption(
                        h5I18nService.text("loan.term." + termOption.value(), termOption.label()),
                        termOption.value()
                ))
                .toList();
    }

    private void validateCalculateRequest(LoanCalculateRequest request) {
        validateAmountAndTerm(request.amount(), request.term());
    }

    private void validateAmountAndTerm(Long amount, Integer term) {
        H5LoanProperties.AmountRange amountRange = h5LoanProperties.amountRange();
        if (amount < amountRange.min() || amount > amountRange.max()) {
            throw new BizException(400, "amount is out of range");
        }
        if ((amount - amountRange.min()) % amountRange.step() != 0) {
            throw new BizException(400, "amount step is invalid");
        }
        boolean supportedTerm = h5LoanProperties.termOptions().stream()
                .anyMatch(termOption -> termOption.value().equals(term));
        if (!supportedTerm) {
            throw new BizException(400, "term is unsupported");
        }
    }

    private List<LoanCalculateResponse.RepaymentPlanItem> mapRepaymentPlan(JsonNode repaymentPlan) {
        if (!repaymentPlan.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(repaymentPlan.spliterator(), false)
                .map(item -> new LoanCalculateResponse.RepaymentPlanItem(
                        item.path("period").asInt(),
                        item.path("date").asText(),
                        centsToYuan(item.path("principal").asLong()),
                        centsToYuan(item.path("interest").asLong()),
                        centsToYuan(item.path("total").asLong())
                ))
                .toList();
    }

    private String readAnnualRate(JsonNode data) {
        JsonNode yearRate = data.path("yearRate");
        if (yearRate.isTextual()) {
            return yearRate.asText();
        }
        if (yearRate.isNumber()) {
            return yearRate.decimalValue().setScale(1, RoundingMode.HALF_UP) + "%";
        }
        return h5LoanProperties.annualRate().multiply(BigDecimal.valueOf(100L))
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private record LoanTrailForwardData(
            String uid,
            String applyId,
            Long loanAmount,
            Integer loanPeriod
    ) {
    }
}
```

Implementation rules:

- Move `getCalculatorConfig()` as-is, including i18n fallback keys.
- Move `calculate(...)` as-is, including `LC` request id generation, Yunka path usage, requested-amount fallback, and repayment-plan mapping.
- Keep the narrower validation inside `LoanCalculatorServiceImpl`. Do not remove `LoanServiceImpl.validateAmountAndTerm(...)` yet because apply still needs it.
- `LoanTrailForwardData` should move into `LoanCalculatorServiceImpl` because only `calculate(...)` uses it after this split.

- [ ] **Step 3: Run the focused test until it passes**

Run:

```bash
mvn -Dtest=LoanCalculatorServiceTest test
```

Expected result:

- `Tests run: 4`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 4: Commit the extracted calculator service**

```bash
git add src/main/java/com/nexusfin/equity/service/LoanCalculatorService.java \
        src/main/java/com/nexusfin/equity/service/impl/LoanCalculatorServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java
git commit -m "refactor: extract loan calculator service"
```

### Task 3: Convert LoanServiceImpl into a Calculator Facade

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- Test: `mvn -Dtest=LoanServiceTest,LoanCalculatorServiceTest test`

- [ ] **Step 1: Add facade-level regression tests in LoanServiceTest before rewiring production**

In `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`:

- add `@Mock private LoanCalculatorService loanCalculatorService;`
- pass `loanCalculatorService` into the `LoanServiceImpl` constructor
- add these two tests:

```java
@Test
void shouldDelegateCalculatorConfigToLoanCalculatorService() {
    LoanCalculatorConfigResponse delegated = new LoanCalculatorConfigResponse(
            new LoanCalculatorConfigResponse.AmountRange(100L, 5000L, 100L, 3000L),
            List.of(new LoanCalculatorConfigResponse.TermOption("3期", 3)),
            BigDecimal.valueOf(0.18),
            "XX商业银行",
            new LoanCalculatorConfigResponse.ReceivingAccount("招商银行", "8648", "acc_001")
    );
    when(loanCalculatorService.getCalculatorConfig()).thenReturn(delegated);

    LoanCalculatorConfigResponse response = loanService.getCalculatorConfig();

    assertThat(response).isSameAs(delegated);
    verify(loanCalculatorService).getCalculatorConfig();
}

@Test
void shouldDelegateCalculateToLoanCalculatorService() {
    LoanCalculateRequest request = new LoanCalculateRequest(3000L, 3);
    LoanCalculateResponse delegated = new LoanCalculateResponse(
            new BigDecimal("123.45"),
            "18.0%",
            List.of()
    );
    when(loanCalculatorService.calculate("mem-001", "user-001", request)).thenReturn(delegated);

    LoanCalculateResponse response = loanService.calculate("mem-001", "user-001", request);

    assertThat(response).isSameAs(delegated);
    verify(loanCalculatorService).calculate("mem-001", "user-001", request);
}
```

Also add imports for `LoanCalculateRequest`, `LoanCalculateResponse`, and `LoanCalculatorConfigResponse`.

- [ ] **Step 2: Run the facade test first and confirm it fails because production constructor/wiring is not updated yet**

Run:

```bash
mvn -Dtest=LoanServiceTest,LoanCalculatorServiceTest test
```

Expected red phase:

- `constructor LoanServiceImpl ... cannot be applied to given types`
- or facade tests fail because delegation is not yet wired

- [ ] **Step 3: Rewire LoanServiceImpl to delegate and remove calculator-only helpers**

Update `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java` with these concrete changes:

```java
private final LoanCalculatorService loanCalculatorService;

public LoanServiceImpl(
        H5LoanProperties h5LoanProperties,
        H5BenefitsProperties h5BenefitsProperties,
        YunkaProperties yunkaProperties,
        YunkaGatewayClient yunkaGatewayClient,
        LoanApplicationMappingRepository loanApplicationMappingRepository,
        BenefitOrderService benefitOrderService,
        H5I18nService h5I18nService,
        AsyncCompensationEnqueueService asyncCompensationEnqueueService,
        XiaohuaGatewayService xiaohuaGatewayService,
        YunkaCallTemplate yunkaCallTemplate,
        LoanCalculatorService loanCalculatorService,
        LoanApprovalQueryService loanApprovalQueryService
) {
    this.h5LoanProperties = h5LoanProperties;
    this.h5BenefitsProperties = h5BenefitsProperties;
    this.yunkaProperties = yunkaProperties;
    this.yunkaGatewayClient = yunkaGatewayClient;
    this.loanApplicationMappingRepository = loanApplicationMappingRepository;
    this.benefitOrderService = benefitOrderService;
    this.h5I18nService = h5I18nService;
    this.asyncCompensationEnqueueService = asyncCompensationEnqueueService;
    this.yunkaCallTemplate = yunkaCallTemplate;
    this.loanCalculatorService = loanCalculatorService;
    this.loanApprovalQueryService = loanApprovalQueryService;
}

@Override
public LoanCalculatorConfigResponse getCalculatorConfig() {
    return loanCalculatorService.getCalculatorConfig();
}

@Override
public LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request) {
    return loanCalculatorService.calculate(memberId, uid, request);
}
```

Then remove the calculator-only private members from `LoanServiceImpl`:

- inject `LoanCalculatorService loanCalculatorService`
- `getCalculatorConfig()` becomes `return loanCalculatorService.getCalculatorConfig();`
- `calculate(...)` becomes `return loanCalculatorService.calculate(memberId, uid, request);`
- remove these calculator-only members from `LoanServiceImpl` after delegation:
  - `mapTermOptions(...)`
  - `validateCalculateRequest(...)`
  - `mapRepaymentPlan(...)`
  - `readAnnualRate(...)`
  - `LoanTrailForwardData`
- keep these in `LoanServiceImpl` because apply still uses them:
  - `validateApplyRequest(...)`
  - `validateAmountAndTerm(...)`
  - `resolveBankCardNum(...)`
  - `resolvePlatformBenefitOrderNo(...)`
  - `readRemark(...)`
  - `readText(...)`
  - `LoanApplyForwardData`

- [ ] **Step 4: Run the focused regression until it passes**

Run:

```bash
mvn -Dtest=LoanServiceTest,LoanCalculatorServiceTest test
```

Expected result:

- `Tests run: 11`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 5: Commit the facade delegation**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanServiceTest.java
git commit -m "refactor: delegate loan calculator facade to extracted service"
```

### Task 4: Final Focused Verification and Structure Check

**Files:**
- Verify only, no intended code change
- Test: `mvn -Dtest=LoanServiceTest,LoanCalculatorServiceTest test`
- Test: `mvn -q -DskipTests compile`

- [ ] **Step 1: Run the focused service regression suite**

```bash
mvn -Dtest=LoanServiceTest,LoanCalculatorServiceTest test
```

Expected:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 2: Run compile for the current phase boundary**

```bash
mvn -q -DskipTests compile
```

Expected:

- exit code `0`

- [ ] **Step 3: Confirm calculator-only helpers no longer live in LoanServiceImpl**

```bash
rg -n "private List<LoanCalculatorConfigResponse.TermOption> mapTermOptions|private void validateCalculateRequest|private List<LoanCalculateResponse.RepaymentPlanItem> mapRepaymentPlan|private String readAnnualRate|private record LoanTrailForwardData" \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java
```

Expected:

- no matches
- exit code `1`

- [ ] **Step 4: Confirm Phase 2B stayed within scope**

Run:

```bash
git show --name-only --format='' HEAD
```

Verify the phase only touched:

- `src/main/java/com/nexusfin/equity/service/LoanCalculatorService.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanCalculatorServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanCalculatorServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

No new edits should appear in:

- `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`
- `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`

---

## Self-Review

- Spec coverage checked: this plan covers only the `LoanCalculatorService` line item from Phase 2 and intentionally leaves `LoanApplicationService`, `LoanApplicationGateway`, payload relocation, and typed enqueue payload for later phases.
- Placeholder scan checked: the plan contains concrete file paths, commands, and test code skeletons; no `TODO` or `TBD` markers remain.
- Type consistency checked: constructor additions, mock names, and method names align with the current `LoanService` / `LoanServiceImpl` structure and with the already-completed `LoanApprovalQueryService` split pattern.
