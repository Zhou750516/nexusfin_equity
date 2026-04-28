# Phase 1B Yunka Call Template Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract a shared service-layer `YunkaCallTemplate` that centralizes Yunka request execution, standard response validation, and begin/success/warn logging, then migrate the four current service callers without changing business behavior.

**Architecture:** This slice stays inside the service layer. Add `service/support/YunkaCallTemplate.java` as a thin wrapper around `YunkaGatewayClient`, with one strict `executeForData(...)` path for the common `code == 0 + data` case and one raw `execute(...)` path for the special callers that still need custom post-processing such as timeout compensation or non-zero-code fallback. Migrate `LoanServiceImpl`, `RepaymentServiceImpl`, `XiaohuaGatewayServiceImpl`, and `YunkaLoanApplyCompensationExecutor` to consume the template, while intentionally leaving broader service splitting and payload relocation for later phases.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java`
- `src/test/java/com/nexusfin/equity/service/support/YunkaCallTemplateTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `docs/superpowers/plans/2026-04-28-phase-1a-shared-utils-extraction.md`
- `src/main/java/com/nexusfin/equity/thirdparty/yunka/YunkaGatewayClient.java`
- `src/main/java/com/nexusfin/equity/exception/BizException.java`
- `src/main/java/com/nexusfin/equity/exception/ErrorCodes.java`
- `src/main/java/com/nexusfin/equity/exception/UpstreamTimeoutException.java`
- `src/main/java/com/nexusfin/equity/util/TraceIdUtil.java`

### Explicitly Out of Scope for This Plan

- any `LoanServiceImpl` / `RepaymentServiceImpl` service splitting
- moving `LoanApplyForwardData`, `LoanQueryForwardData`, `Repay*ForwardData` records into a dedicated payload package
- replacing `AsyncCompensationEnqueueService.enqueue(...)` JSON string with a typed payload
- AOP-based gateway logging
- any H5 or joint-login feature work

### Validation Commands

- `mvn -Dtest=YunkaCallTemplateTest test`
- `mvn -Dtest=LoanServiceTest,RepaymentServiceTest,XiaohuaGatewayServiceTest,YunkaLoanApplyCompensationExecutorTest,YunkaCallTemplateTest test`
- `mvn -q -DskipTests compile`
- `rg -n "requireSuccessfulYunkaData|elapsedMs\\(" src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`

---

### Task 1: Add `YunkaCallTemplate` with Unit Tests

**Files:**
- Create: `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java`
- Create: `src/test/java/com/nexusfin/equity/service/support/YunkaCallTemplateTest.java`
- Test: `mvn -Dtest=YunkaCallTemplateTest test`

- [ ] **Step 1: Write the failing unit test for the template**

Create `src/test/java/com/nexusfin/equity/service/support/YunkaCallTemplateTest.java` with this exact content:

```java
package com.nexusfin.equity.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
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
class YunkaCallTemplateTest {

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Test
    void shouldExecuteStrictDataCallAndForwardGatewayRequest() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        JsonNode data = JsonNodeFactory.instance.objectNode().put("loanId", "LN-001");
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", data));

        JsonNode response = template.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan query",
                        "LQ-001",
                        "/loan/query",
                        "APP-001",
                        JsonNodeFactory.instance.objectNode().put("uid", "user-001")
                ).withMemberId("mem-001")
        );

        assertThat(response.path("loanId").asText()).isEqualTo("LN-001");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        assertThat(captor.getValue().requestId()).isEqualTo("LQ-001");
        assertThat(captor.getValue().path()).isEqualTo("/loan/query");
        assertThat(captor.getValue().bizOrderNo()).isEqualTo("APP-001");
    }

    @Test
    void shouldConvertMissingDataIntoEmptyObjectNode() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", null));

        JsonNode response = template.executeForData(
                YunkaCallTemplate.YunkaCall.of("repay trial", "RT-001", "/repay/trial", "LN-001", new Object())
        );

        assertThat(response.isObject()).isTrue();
        assertThat(response.isEmpty()).isTrue();
    }

    @Test
    void shouldThrowBizExceptionWhenStrictDataCallIsRejected() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(10003, "invalid state", null));

        assertThatThrownBy(() -> template.executeForData(
                YunkaCallTemplate.YunkaCall.of("benefit sync", "SYNC-001", "/benefit/sync", "BEN-001", new Object())
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo(ErrorCodes.YUNKA_UPSTREAM_REJECTED);
    }

    @Test
    void shouldExposeRawResponseForCustomPostProcessing() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(7002, "processing", JsonNodeFactory.instance.objectNode()));

        YunkaGatewayClient.YunkaGatewayResponse response = template.execute(
                YunkaCallTemplate.YunkaCall.of("loan apply", "LA-001", "/loan/apply", "APP-001", new Object())
        );

        assertThat(response.code()).isEqualTo(7002);
        assertThat(response.message()).isEqualTo("processing");
        assertThat(template.isSuccessful(response)).isFalse();
        assertThat(template.hasData(response)).isFalse();
    }
}
```

- [ ] **Step 2: Run the new test and confirm it fails because the template does not exist yet**

Run:

```bash
mvn -Dtest=YunkaCallTemplateTest test
```

Expected: compilation fails with `cannot find symbol` for `YunkaCallTemplate`.

- [ ] **Step 3: Implement `YunkaCallTemplate`**

Create `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java` with this exact content:

```java
package com.nexusfin.equity.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.TraceIdUtil;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YunkaCallTemplate {

    private static final Logger log = LoggerFactory.getLogger(YunkaCallTemplate.class);

    private final YunkaGatewayClient yunkaGatewayClient;

    public YunkaCallTemplate(YunkaGatewayClient yunkaGatewayClient) {
        this.yunkaGatewayClient = yunkaGatewayClient;
    }

    public YunkaGatewayClient.YunkaGatewayResponse execute(YunkaCall call) {
        return execute(call, this::requirePresentResponse);
    }

    public JsonNode executeForData(YunkaCall call) {
        return execute(call, this::requireSuccessfulData);
    }

    public <T> T execute(YunkaCall call, Function<YunkaGatewayClient.YunkaGatewayResponse, T> mapper) {
        long startNanos = System.nanoTime();
        log.info(
                "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} yunka request begin",
                TraceIdUtil.getTraceId(),
                call.bizOrderNo(),
                call.requestId(),
                normalize(call.memberId()),
                normalize(call.benefitOrderNo()),
                call.path(),
                call.scene()
        );
        try {
            YunkaGatewayClient.YunkaGatewayResponse response = yunkaGatewayClient.proxy(call.toRequest());
            T result = mapper.apply(response);
            log.info(
                    "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} elapsedMs={} yunka request success",
                    TraceIdUtil.getTraceId(),
                    call.bizOrderNo(),
                    call.requestId(),
                    normalize(call.memberId()),
                    normalize(call.benefitOrderNo()),
                    call.path(),
                    call.scene(),
                    elapsedMs(startNanos)
            );
            return result;
        } catch (BizException exception) {
            log.warn(
                    "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    call.bizOrderNo(),
                    call.requestId(),
                    normalize(call.memberId()),
                    normalize(call.benefitOrderNo()),
                    call.path(),
                    call.scene(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg()
            );
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    call.bizOrderNo(),
                    call.requestId(),
                    normalize(call.memberId()),
                    normalize(call.benefitOrderNo()),
                    call.path(),
                    call.scene(),
                    elapsedMs(startNanos),
                    ErrorCodes.YUNKA_UPSTREAM_FAILED,
                    defaultMessage(exception)
            );
            throw exception;
        }
    }

    public boolean isSuccessful(YunkaGatewayClient.YunkaGatewayResponse response) {
        return response != null && response.code() == 0;
    }

    public boolean hasData(YunkaGatewayClient.YunkaGatewayResponse response) {
        return isSuccessful(response)
                && response.data() != null
                && !response.data().isNull()
                && !response.data().isMissingNode();
    }

    public YunkaGatewayClient.YunkaGatewayResponse requirePresentResponse(
            YunkaGatewayClient.YunkaGatewayResponse response
    ) {
        if (response == null) {
            throw new BizException(ErrorCodes.YUNKA_RESPONSE_EMPTY, "Yunka gateway response is empty");
        }
        return response;
    }

    public JsonNode requireSuccessfulData(YunkaGatewayClient.YunkaGatewayResponse response) {
        YunkaGatewayClient.YunkaGatewayResponse presentResponse = requirePresentResponse(response);
        if (presentResponse.code() != 0) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, presentResponse.message());
        }
        return presentResponse.data() == null ? JsonNodeFactory.instance.objectNode() : presentResponse.data();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String defaultMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public record YunkaCall(
            String scene,
            String requestId,
            String path,
            String bizOrderNo,
            Object payload,
            String memberId,
            String benefitOrderNo
    ) {

        public static YunkaCall of(String scene, String requestId, String path, String bizOrderNo, Object payload) {
            return new YunkaCall(scene, requestId, path, bizOrderNo, payload, null, null);
        }

        public YunkaCall withMemberId(String memberId) {
            return new YunkaCall(scene, requestId, path, bizOrderNo, payload, memberId, benefitOrderNo);
        }

        public YunkaCall withBenefitOrderNo(String benefitOrderNo) {
            return new YunkaCall(scene, requestId, path, bizOrderNo, payload, memberId, benefitOrderNo);
        }

        public YunkaGatewayClient.YunkaGatewayRequest toRequest() {
            return new YunkaGatewayClient.YunkaGatewayRequest(requestId, path, bizOrderNo, payload);
        }
    }
}
```

- [ ] **Step 4: Run the focused unit test and confirm the new template passes**

Run:

```bash
mvn -Dtest=YunkaCallTemplateTest test
```

Expected: `YunkaCallTemplateTest` passes.

- [ ] **Step 5: Commit the template-only slice**

```bash
git add src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java \
  src/test/java/com/nexusfin/equity/service/support/YunkaCallTemplateTest.java
git commit -m "refactor: add yunka call template"
```

---

### Task 2: Migrate `LoanServiceImpl` and `RepaymentServiceImpl` to the Template

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`
- Test: `mvn -Dtest=LoanServiceTest,RepaymentServiceTest,YunkaCallTemplateTest test`

- [ ] **Step 1: Add regression coverage for `LoanServiceImpl.apply(...)` timeout and rejected-response branches**

Append these two tests to `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`:

```java
    @Test
    void shouldReturnFailedResponseWhenLoanApplyIsRejected() throws Exception {
        when(benefitOrderService.createOrder(eq("mem-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-REJECT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                10003,
                "invalid loan state",
                objectMapper.readTree("{}")
        ));

        LoanApplyResponse response = loanService.apply("mem-001", "user-001", buildApplyRequest());

        assertThat(response.applicationId()).isNull();
        assertThat(response.status()).isEqualTo("loan_failed");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-REJECT");
        assertThat(response.message()).isEqualTo("权益购买成功，借款申请失败：invalid loan state");
    }

    @Test
    void shouldEnqueueCompensationWhenLoanApplyTimesOut() {
        when(benefitOrderService.createOrder(eq("mem-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-TIMEOUT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenThrow(new com.nexusfin.equity.exception.UpstreamTimeoutException("Yunka gateway timeout"));

        LoanApplyResponse response = loanService.apply("mem-001", "user-001", buildApplyRequest());

        assertThat(response.status()).isEqualTo("pending");
        verify(asyncCompensationEnqueueService).enqueue(any());
    }
```

Also add this helper method near the bottom of the same test file so all three apply tests use the same payload:

```java
    private LoanApplyRequest buildApplyRequest() throws Exception {
        return new LoanApplyRequest(
                3000L,
                3,
                "acc_001",
                List.of("loan", "user"),
                "rent",
                "DAILY_CONSUMPTION",
                "6222020202028648",
                objectMapper.readTree("""
                        {"education":"BACHELOR","monthlyIncome":"10000-15000"}
                        """),
                objectMapper.readTree("""
                        {"cidExpireDate":"2036-01-01"}
                        """),
                objectMapper.readTree("""
                        [{"name":"张三","mobile":"13800000001","relation":"SPOUSE"}]
                        """),
                objectMapper.readTree("""
                        {"occupation":"ENGINEER"}
                        """),
                objectMapper.readTree("""
                        {"channel":"ABS_H5"}
                        """),
                objectMapper.readTree("""
                        [{"type":"FACE","base64":"abc"}]
                        """),
                "PBEN-001"
        );
    }
```

Then replace the existing inline `new LoanApplyRequest(...)` construction in `shouldForwardRichApplyFieldsWhileKeepingCurrentPayloadCompatible()` with:

```java
        LoanApplyRequest request = buildApplyRequest();
```

- [ ] **Step 2: Run the focused service tests and confirm the new tests fail before the refactor**

Run:

```bash
mvn -Dtest=LoanServiceTest,RepaymentServiceTest,YunkaCallTemplateTest test
```

Expected: at least one new `LoanServiceTest` assertion fails until the template migration is complete.

- [ ] **Step 3: Inject `YunkaCallTemplate` into both services**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`:

1. Add the import:

```java
import com.nexusfin.equity.service.support.YunkaCallTemplate;
```

2. Add the field:

```java
    private final YunkaCallTemplate yunkaCallTemplate;
```

3. Extend the constructor signature and assignment:

```java
            AsyncCompensationEnqueueService asyncCompensationEnqueueService,
            XiaohuaGatewayService xiaohuaGatewayService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
```

```java
        this.yunkaCallTemplate = yunkaCallTemplate;
```

Apply the same pattern to `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`:

```java
import com.nexusfin.equity.service.support.YunkaCallTemplate;
```

```java
    private final YunkaCallTemplate yunkaCallTemplate;
```

```java
            H5I18nService h5I18nService,
            XiaohuaGatewayService xiaohuaGatewayService,
            MemberInfoRepository memberInfoRepository,
            SensitiveDataCipher sensitiveDataCipher,
            YunkaCallTemplate yunkaCallTemplate
    ) {
```

```java
        this.yunkaCallTemplate = yunkaCallTemplate;
```

- [ ] **Step 4: Replace the duplicated Yunka request blocks in `LoanServiceImpl`**

Replace the current `calculate(...)` request/log/try/catch block with:

```java
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan calculate",
                        requestId,
                        yunkaProperties.paths().loanCalculate(),
                        requestId,
                        new LoanTrailForwardData(uid, requestId, yuanToCent(request.amount()), request.term())
                ).withMemberId(memberId)
        );
```

Replace the current `queryLoan(...)` request/log/try/catch block with:

```java
        return yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan query",
                        requestId,
                        yunkaProperties.paths().loanQuery(),
                        mapping.getApplicationId(),
                        new LoanQueryForwardData(mapping.getExternalUserId(), mapping.getUpstreamQueryValue())
                ).withMemberId(mapping.getMemberId())
        );
```

Replace the direct `yunkaGatewayClient.proxy(...)` call inside `apply(...)` with:

```java
            response = yunkaCallTemplate.execute(
                    YunkaCallTemplate.YunkaCall.of(
                            "loan apply",
                            requestId,
                            yunkaProperties.paths().loanApply(),
                            applicationId,
                            forwardData
                    ).withMemberId(memberId).withBenefitOrderNo(benefitOrder.benefitOrderNo())
            );
```

Then simplify the rejected-response branch immediately after that call to:

```java
        if (!yunkaCallTemplate.isSuccessful(response)) {
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), response.message());
        }
```

Delete these private methods from `LoanServiceImpl.java` completely:

```java
    private JsonNode requireSuccessfulYunkaData(YunkaGatewayClient.YunkaGatewayResponse response) {
        if (response == null || response.code() != 0) {
            String message = response == null ? "Yunka gateway response is empty" : response.message();
            throw new BizException(502, message);
        }
        return response.data();
    }
```

```java
    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
```

Do not change:

- the timeout compensation enqueue payload
- `saveApplicationMapping(...)`
- `buildLoanFailedResponse(...)`
- any DTO shape or returned status mapping

- [ ] **Step 5: Replace the duplicated Yunka request blocks in `RepaymentServiceImpl`**

Replace the `getInfo(...)` request/log/try/catch block with:

```java
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "repayment info",
                        requestId,
                        yunkaProperties.paths().repayTrial(),
                        loanId,
                        new RepayTrialForwardData(uid, loanId, DEFAULT_REPAY_TYPE, List.of())
                )
        );
```

Replace the `submit(...)` request/log/try/catch block with:

```java
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "repayment submit",
                        requestId,
                        yunkaProperties.paths().repayApply(),
                        request.loanId(),
                        new RepayApplyForwardData(
                                uid,
                                request.loanId(),
                                mapRepayType(request.repaymentType()),
                                List.of(),
                                bankCardNum,
                                yuanToCent(request.amount())
                        )
                )
        );
```

Replace the `getResult(...)` request/log/try/catch block with:

```java
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "repayment result",
                        requestId,
                        yunkaProperties.paths().repayQuery(),
                        repaymentId,
                        new RepayQueryForwardData(uid, repaymentId)
                )
        );
```

Delete these private methods from `RepaymentServiceImpl.java` completely:

```java
    private JsonNode requireSuccessfulYunkaData(YunkaGatewayClient.YunkaGatewayResponse response) {
        if (response == null || response.code() != 0) {
            String message = response == null ? "Yunka gateway response is empty" : response.message();
            throw new BizException(502, message);
        }
        return response.data();
    }
```

```java
    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
```

- [ ] **Step 6: Update the two service tests for constructor injection**

In `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`, change the constructor call to:

```java
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
                new YunkaCallTemplate(yunkaGatewayClient)
        );
```

Add this import:

```java
import com.nexusfin.equity.service.support.YunkaCallTemplate;
```

In `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`, change the constructor call to:

```java
        repaymentService = new RepaymentServiceImpl(
                h5LoanProperties(),
                yunkaProperties(),
                yunkaGatewayClient,
                h5I18nService,
                xiaohuaGatewayService,
                memberInfoRepository,
                sensitiveDataCipher,
                new YunkaCallTemplate(yunkaGatewayClient)
        );
```

Add the same import there.

- [ ] **Step 7: Run the focused service tests and confirm the migration is behaviorally stable**

Run:

```bash
mvn -Dtest=LoanServiceTest,RepaymentServiceTest,YunkaCallTemplateTest test
```

Expected: all selected tests pass.

- [ ] **Step 8: Commit the two-service migration**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java \
  src/test/java/com/nexusfin/equity/service/LoanServiceTest.java \
  src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java
git commit -m "refactor: route loan and repayment yunka calls through template"
```

---

### Task 3: Migrate `XiaohuaGatewayServiceImpl` and `YunkaLoanApplyCompensationExecutor`

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`
- Modify: `src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java`
- Test: `mvn -Dtest=XiaohuaGatewayServiceTest,YunkaLoanApplyCompensationExecutorTest,YunkaCallTemplateTest test`

- [ ] **Step 1: Add a compensation regression test for the fallback-to-apply path**

Append this test to `src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java`:

```java
    @Test
    void shouldCallLoanApplyWhenPendingReviewQueryHasNoUsableData() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new com.nexusfin.equity.service.support.YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-fallback");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-005",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-005",
                  "uid": "tech-user-005",
                  "benefitOrderNo": "ord-005",
                  "applyId": "APP-005",
                  "loanId": "LN-005",
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_005"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-005");
        mapping.setExternalUserId("tech-user-005");
        mapping.setUpstreamQueryValue("LN-005");
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-005")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", null))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("loanId", "LN-005")
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("LN-005");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).path()).isEqualTo("/loan/query");
        assertThat(requestCaptor.getAllValues().get(1).path()).isEqualTo("/loan/apply");
    }
```

- [ ] **Step 2: Run the focused tests and confirm the new compensation test fails before the refactor**

Run:

```bash
mvn -Dtest=XiaohuaGatewayServiceTest,YunkaLoanApplyCompensationExecutorTest,YunkaCallTemplateTest test
```

Expected: the new fallback test fails until the executor migration is finished.

- [ ] **Step 3: Inject `YunkaCallTemplate` into `XiaohuaGatewayServiceImpl` and delegate all gateway calls through it**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`:

1. Add the import:

```java
import com.nexusfin.equity.service.support.YunkaCallTemplate;
```

2. Replace the `YunkaGatewayClient` field with:

```java
    private final YunkaCallTemplate yunkaCallTemplate;
```

3. Update the constructor signature and assignment:

```java
            YunkaCallTemplate yunkaCallTemplate,
            YunkaProperties yunkaProperties,
            ObjectMapper objectMapper
    ) {
```

```java
        this.yunkaCallTemplate = yunkaCallTemplate;
```

4. Replace the private `execute(...)` method with:

```java
    private JsonNode execute(String requestId, String path, String bizOrderNo, Object payload) {
        return yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of("xiaohua gateway", requestId, path, bizOrderNo, payload)
        );
    }
```

Delete the old inline response validation block entirely.

- [ ] **Step 4: Inject `YunkaCallTemplate` into `YunkaLoanApplyCompensationExecutor` and route both query/apply branches through it**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`:

1. Add these imports:

```java
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
```

2. Replace the `YunkaGatewayClient yunkaGatewayClient` field with:

```java
    private final YunkaCallTemplate yunkaCallTemplate;
```

3. Update the constructor signature and assignment:

```java
            YunkaCallTemplate yunkaCallTemplate,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            YunkaProperties yunkaProperties,
            ObjectMapper objectMapper
    ) {
```

```java
        this.yunkaCallTemplate = yunkaCallTemplate;
```

4. Replace the apply retry call in `execute(...)` with:

```java
        YunkaGatewayClient.YunkaGatewayResponse response = yunkaCallTemplate.execute(
                YunkaCallTemplate.YunkaCall.of(
                        "yunka loan apply retry apply",
                        payload.requestId(),
                        payload.path(),
                        payload.bizOrderNo(),
                        new YunkaLoanApplyForwardData(
                                payload.uid(),
                                payload.benefitOrderNo(),
                                payload.applyId(),
                                payload.loanId(),
                                payload.loanAmount(),
                                payload.loanPeriod(),
                                payload.bankCardNo()
                        )
                )
        );
```

5. Immediately after that call, keep the existing business semantics by converting only non-zero responses into a BizException:

```java
        if (!yunkaCallTemplate.isSuccessful(response)) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, response.message());
        }
```

6. Replace `queryExisting(...)` with:

```java
    private YunkaGatewayClient.YunkaGatewayResponse queryExisting(
            LoanApplicationMapping mapping,
            YunkaLoanApplyPayload payload
    ) {
        String externalUserId = mapping.getExternalUserId() == null || mapping.getExternalUserId().isBlank()
                ? payload.uid()
                : mapping.getExternalUserId();
        String upstreamQueryValue = mapping.getUpstreamQueryValue() == null || mapping.getUpstreamQueryValue().isBlank()
                ? payload.loanId()
                : mapping.getUpstreamQueryValue();
        ObjectNode queryData = objectMapper.createObjectNode();
        queryData.put("uid", externalUserId);
        queryData.put("loanId", upstreamQueryValue);
        return yunkaCallTemplate.execute(
                YunkaCallTemplate.YunkaCall.of(
                        "yunka loan apply retry query",
                        payload.requestId(),
                        yunkaProperties.paths().loanQuery(),
                        payload.applyId(),
                        queryData
                )
        );
    }
```

7. Keep the pending-review fast path, but switch the null/code/data predicate to the template helper:

```java
                if (yunkaCallTemplate.hasData(queryResponse)) {
                    markMappingActive(payload.applyId(), queryResponse);
                    return new ExecutionResult(writeResponse(queryResponse));
                }
```

- [ ] **Step 5: Update the two tests for constructor injection**

In `src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java`, change setup to:

```java
        gatewayService = new XiaohuaGatewayServiceImpl(
                new YunkaCallTemplate(yunkaGatewayClient),
                yunkaProperties(),
                objectMapper
        );
```

Add this import:

```java
import com.nexusfin.equity.service.support.YunkaCallTemplate;
```

In `src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java`, replace every constructor call with:

```java
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
```

Add the same import there.

- [ ] **Step 6: Run the focused tests and confirm both migrated callers are stable**

Run:

```bash
mvn -Dtest=XiaohuaGatewayServiceTest,YunkaLoanApplyCompensationExecutorTest,YunkaCallTemplateTest test
```

Expected: all selected tests pass.

- [ ] **Step 7: Commit the gateway-service and compensation migration**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java \
  src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java \
  src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java
git commit -m "refactor: route remaining yunka callers through template"
```

---

### Task 4: Final Verification and Cleanup Checks

**Files:**
- Verify only: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Verify only: `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- Verify only: `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
- Verify only: `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`
- Verify only: `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- Verify only: `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`
- Verify only: `src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java`
- Verify only: `src/test/java/com/nexusfin/equity/service/YunkaLoanApplyCompensationExecutorTest.java`
- Verify only: `src/test/java/com/nexusfin/equity/service/support/YunkaCallTemplateTest.java`

- [ ] **Step 1: Run the full focused regression suite**

Run:

```bash
mvn -Dtest=LoanServiceTest,RepaymentServiceTest,XiaohuaGatewayServiceTest,YunkaLoanApplyCompensationExecutorTest,YunkaCallTemplateTest test
```

Expected: all selected tests pass.

- [ ] **Step 2: Run compile-only verification**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compile exits with code `0`.

- [ ] **Step 3: Verify the four target classes no longer define duplicated local Yunka helpers**

Run:

```bash
rg -n "requireSuccessfulYunkaData|elapsedMs\\(" \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java
```

Expected: no matches.

- [ ] **Step 4: Commit the final verification state if any follow-up edits were needed during cleanup**

If verification required no code edits, do not create an empty commit.

If a final cleanup edit was needed, commit only that delta:

```bash
git add <exact files touched during cleanup>
git commit -m "refactor: finalize yunka call template cleanup"
```

---

## Self-Review

- Spec coverage: This plan covers the `Phase 1` item for `service/support/YunkaCallTemplate.java` and the four targeted callers, while intentionally deferring service splitting, payload relocation, and compensation payload typing to later phases.
- Placeholder scan: No `TODO`, `TBD`, or "similar to above" placeholders remain.
- Type consistency: The same `YunkaCallTemplate` constructor and `YunkaCallTemplate.YunkaCall.of(...)` usage pattern is used across all implementation and test tasks.
