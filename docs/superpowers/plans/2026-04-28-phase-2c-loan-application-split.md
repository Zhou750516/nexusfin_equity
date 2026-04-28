# Phase 2C Loan Application Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the loan apply-side responsibilities out of `LoanServiceImpl` into a dedicated service while keeping the `LoanService` interface, `LoanController` entrypoints, request/response shapes, apply behavior, timeout compensation behavior, and current logging/exception semantics unchanged.

**Architecture:** Introduce a dedicated `LoanApplicationService` inside the existing service layer. `LoanServiceImpl` becomes a thin facade across calculator, apply, and approval-query paths. The new application service owns request validation, benefit order creation, Yunka `/loan/apply` execution, mapping persistence, timeout compensation enqueue, and apply response shaping. This phase intentionally does not introduce `LoanApplicationGateway`, typed enqueue payloads, or payload package relocation.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/service/LoanApplicationService.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `docs/superpowers/plans/2026-04-28-phase-2b-loan-calculator-split.md`
- `src/main/java/com/nexusfin/equity/service/LoanService.java`
- `src/main/java/com/nexusfin/equity/controller/LoanController.java`
- `src/main/java/com/nexusfin/equity/service/AsyncCompensationEnqueueService.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

### Explicitly In Scope

- `LoanServiceImpl.apply(...)`
- apply-only helper logic directly serving the apply path
- benefit order creation in the apply path
- Yunka `/loan/apply` forwarding and response handling
- timeout compensation enqueue in the apply path
- mapping persistence in the apply path
- facade delegation from `LoanServiceImpl` to the new application service

### Explicitly Out of Scope

- any `LoanCalculatorService` behavior change
- any `LoanApprovalQueryService` behavior change
- any `RepaymentServiceImpl` split
- introducing `LoanApplicationGateway`
- moving `LoanApplyForwardData` to `thirdparty/yunka/payload/`
- converting `AsyncCompensationEnqueueService.EnqueueCommand` to typed payloads
- any new changes in `XiaohuaGatewayServiceImpl`
- any new changes in `YunkaLoanApplyCompensationExecutor`
- any Phase 3 `thirdparty/qw` consolidation

### Boundary Notes

- Keep `LoanService` unchanged.
- Keep `LoanController` unchanged.
- `LoanServiceImpl` should become a pure facade after this phase, delegating all four public methods to `LoanCalculatorService`, `LoanApplicationService`, and `LoanApprovalQueryService`.
- Move `@Transactional(noRollbackFor = BenefitPurchaseSyncTimeoutCompensationException.class)` from `LoanServiceImpl.apply(...)` to `LoanApplicationServiceImpl.apply(...)`.
- Keep the raw JSON string passed to `AsyncCompensationEnqueueService.EnqueueCommand` exactly as-is in semantics. Do not widen into typed payload work in this phase.
- Keep the current rejected / timeout / empty-response business behavior unchanged:
  - rejected still returns `applicationId = null`, `status = "loan_failed"`
  - timeout still returns `status = "pending"` and enqueues async compensation
  - `buildLoanFailedResponse(...)` wording and i18n fallback remain unchanged

### Validation Commands

- `mvn -Dtest=LoanApplicationServiceTest test`
- `mvn -Dtest=LoanServiceTest,LoanApplicationServiceTest test`
- `mvn -q -DskipTests compile`
- `rg -n "private void validateApplyRequest|private void validateAmountAndTerm|private LoanApplyResponse buildLoanFailedResponse|private void saveApplicationMapping|private String resolveBankCardNum|private String resolvePlatformBenefitOrderNo|private record LoanApplyForwardData" src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`

---

### Task 1: Freeze Loan Apply Behavior with Focused Service Tests

**Files:**
- Create: `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- Test: `mvn -Dtest=LoanApplicationServiceTest test`

- [ ] **Step 1: Write the failing apply-service tests before introducing the service**

Create `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java` with this concrete test class:

```java
package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.impl.LoanApplicationServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LoanApplicationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Mock
    private BenefitOrderService benefitOrderService;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private AsyncCompensationEnqueueService asyncCompensationEnqueueService;

    private LoanApplicationService loanApplicationService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        loanApplicationService = new LoanApplicationServiceImpl(
                h5LoanProperties(),
                h5BenefitsProperties(),
                yunkaProperties(),
                loanApplicationMappingRepository,
                benefitOrderService,
                h5I18nService,
                asyncCompensationEnqueueService,
                new YunkaCallTemplate(yunkaGatewayClient)
        );
    }

    @Test
    void shouldForwardRichApplyFieldsAndCreateActiveMappingOnSuccessfulLoanApply() throws Exception {
        when(benefitOrderService.createOrder(eq("mem-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-001", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "loanId": "LN-UPSTREAM-001",
                          "status": "4002",
                          "remark": "处理中"
                        }
                        """)
        ));

        LoanApplyResponse response = loanApplicationService.apply("mem-001", "user-001", buildApplyRequest());

        assertThat(response.applicationId()).startsWith("APP-");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-001");
        assertThat(response.message()).isEqualTo("处理中");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> yunkaCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(yunkaCaptor.capture());
        JsonNode forwardData = objectMapper.valueToTree(yunkaCaptor.getValue().data());
        assertThat(forwardData.path("purpose").asText()).isEqualTo("rent");
        assertThat(forwardData.path("loanReason").asText()).isEqualTo("DAILY_CONSUMPTION");
        assertThat(forwardData.path("bankCardNum").asText()).isEqualTo("6222020202028648");
        assertThat(forwardData.path("platformBenefitOrderNo").asText()).isEqualTo("PBEN-001");
        assertThat(forwardData.path("basicInfo").path("education").asText()).isEqualTo("BACHELOR");
        assertThat(forwardData.path("contactInfo").isArray()).isTrue();
        assertThat(forwardData.path("imageInfo").isArray()).isTrue();

        ArgumentCaptor<LoanApplicationMapping> mappingCaptor =
                ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).insert(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getApplicationId()).isEqualTo(response.applicationId());
        assertThat(mappingCaptor.getValue().getBenefitOrderNo()).isEqualTo("BEN-001");
        assertThat(mappingCaptor.getValue().getUpstreamQueryValue()).isEqualTo("LN-UPSTREAM-001");
        assertThat(mappingCaptor.getValue().getPurpose()).isEqualTo("rent");
        assertThat(mappingCaptor.getValue().getMappingStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldReturnFailedResponseWhenLoanApplyIsRejected(CapturedOutput output) throws Exception {
        when(benefitOrderService.createOrder(eq("mem-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-REJECT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                10003,
                "invalid loan state",
                objectMapper.readTree("{}")
        ));

        LoanApplyResponse response = loanApplicationService.apply("mem-001", "user-001", buildApplyRequest());

        assertThat(response.applicationId()).isNull();
        assertThat(response.status()).isEqualTo("loan_failed");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-REJECT");
        assertThat(response.message()).isEqualTo("权益购买成功，借款申请失败：invalid loan state");
        assertThat(output)
                .contains("scene=loan apply")
                .contains("errorNo=YUNKA_UPSTREAM_REJECTED")
                .doesNotContain("yunka request success");
    }

    @Test
    void shouldEnqueueCompensationAndSavePendingReviewMappingWhenLoanApplyTimesOut() throws Exception {
        when(benefitOrderService.createOrder(eq("mem-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-TIMEOUT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenThrow(new UpstreamTimeoutException("Yunka gateway timeout"));

        LoanApplyResponse response = loanApplicationService.apply("mem-001", "user-001", buildApplyRequest());

        assertThat(response.applicationId()).startsWith("APP-");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-TIMEOUT");

        ArgumentCaptor<LoanApplicationMapping> mappingCaptor =
                ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).insert(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getApplicationId()).isEqualTo(response.applicationId());
        assertThat(mappingCaptor.getValue().getBenefitOrderNo()).isEqualTo("BEN-TIMEOUT");
        assertThat(mappingCaptor.getValue().getMappingStatus()).isEqualTo("PENDING_REVIEW");

        ArgumentCaptor<AsyncCompensationEnqueueService.EnqueueCommand> enqueueCaptor =
                ArgumentCaptor.forClass(AsyncCompensationEnqueueService.EnqueueCommand.class);
        verify(asyncCompensationEnqueueService).enqueue(enqueueCaptor.capture());
        assertThat(enqueueCaptor.getValue().taskType()).isEqualTo("YUNKA_LOAN_APPLY_RETRY");
        assertThat(enqueueCaptor.getValue().bizKey()).isEqualTo("LOAN_APPLY:" + response.applicationId());
        assertThat(enqueueCaptor.getValue().bizOrderNo()).isEqualTo(response.applicationId());
        assertThat(enqueueCaptor.getValue().requestPath()).isEqualTo("/api/gateway/proxy");
        JsonNode payload = objectMapper.readTree(enqueueCaptor.getValue().requestPayload());
        assertThat(payload.path("path").asText()).isEqualTo("/loan/apply");
        assertThat(payload.path("benefitOrderNo").asText()).isEqualTo("BEN-TIMEOUT");
        assertThat(payload.path("uid").asText()).isEqualTo("user-001");
    }

    @Test
    void shouldRejectUnsupportedReceivingAccountBeforeCreatingBenefitOrder() throws Exception {
        LoanApplyRequest invalidRequest = new LoanApplyRequest(
                3000L,
                3,
                "acc_invalid",
                List.of("loan", "user"),
                "rent",
                "DAILY_CONSUMPTION",
                "6222020202028648",
                objectMapper.readTree("{\"education\":\"BACHELOR\"}"),
                objectMapper.readTree("{\"cidExpireDate\":\"2036-01-01\"}"),
                objectMapper.readTree("[{\"name\":\"张三\",\"mobile\":\"13800000001\",\"relation\":\"SPOUSE\"}]"),
                objectMapper.readTree("{\"occupation\":\"ENGINEER\"}"),
                objectMapper.readTree("{\"channel\":\"ABS_H5\"}"),
                objectMapper.readTree("[{\"type\":\"FACE\",\"base64\":\"abc\"}]"),
                "PBEN-001"
        );

        assertThatThrownBy(() -> loanApplicationService.apply("mem-001", "user-001", invalidRequest))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("receiving account is unsupported");

        verifyNoInteractions(benefitOrderService, asyncCompensationEnqueueService, yunkaGatewayClient);
    }

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

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new H5LoanProperties.TermOption("3期", 3)),
                BigDecimal.valueOf(0.18),
                "XX商业银行",
                new H5LoanProperties.ReceivingAccount("招商银行", "8648", "acc_001")
        );
    }

    private H5BenefitsProperties h5BenefitsProperties() {
        return new H5BenefitsProperties(
                "HUXUAN_CARD",
                new H5BenefitsProperties.Activate(30000L, "huixuan_card", "惠选卡开通成功"),
                new H5BenefitsProperties.Detail(
                        "惠选卡",
                        300L,
                        448L,
                        List.of(new H5BenefitsProperties.Feature("f1", "d1")),
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

- [ ] **Step 2: Run the focused test and confirm it fails because the new service does not exist yet**

Run:

```bash
mvn -Dtest=LoanApplicationServiceTest test
```

Expected failure shape:

- `cannot find symbol`
- `LoanApplicationService`
- `LoanApplicationServiceImpl`

- [ ] **Step 3: Commit the red test only**

```bash
git add src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java
git commit -m "test: freeze loan application service behavior"
```

### Task 2: Introduce the Application Service and Move Apply Logic

**Files:**
- Create: `src/main/java/com/nexusfin/equity/service/LoanApplicationService.java`
- Create: `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- Test: `mvn -Dtest=LoanApplicationServiceTest test`

- [ ] **Step 1: Add the service interface**

Create `src/main/java/com/nexusfin/equity/service/LoanApplicationService.java`:

```java
package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.LoanApplyResponse;

public interface LoanApplicationService {

    LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request);
}
```

- [ ] **Step 2: Implement the application service with the current apply semantics**

Create `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java` with this structure:

```java
package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BenefitPurchaseSyncTimeoutCompensationException;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanApplicationService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);
    private static final String DEFAULT_CHANNEL_CODE = "KJ";

    private final H5LoanProperties h5LoanProperties;
    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final BenefitOrderService benefitOrderService;
    private final H5I18nService h5I18nService;
    private final AsyncCompensationEnqueueService asyncCompensationEnqueueService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanApplicationServiceImpl(
            H5LoanProperties h5LoanProperties,
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            BenefitOrderService benefitOrderService,
            H5I18nService h5I18nService,
            AsyncCompensationEnqueueService asyncCompensationEnqueueService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.benefitOrderService = benefitOrderService;
        this.h5I18nService = h5I18nService;
        this.asyncCompensationEnqueueService = asyncCompensationEnqueueService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    @Transactional(noRollbackFor = BenefitPurchaseSyncTimeoutCompensationException.class)
    public LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request) {
        validateApplyRequest(request);
        String applicationId = next("APP");
        String loanId = next("LN");
        CreateBenefitOrderResponse benefitOrder = benefitOrderService.createOrder(
                memberId,
                new CreateBenefitOrderRequest(
                        "loan-apply-" + applicationId,
                        h5BenefitsProperties.productCode(),
                        yuanToCent(request.amount()),
                        Boolean.TRUE
                )
        );
        YunkaGatewayClient.YunkaGatewayResponse response;
        String requestId = next("LA");
        String upstreamBankCardNum = resolveBankCardNum(request);
        String platformBenefitOrderNo = resolvePlatformBenefitOrderNo(
                benefitOrder.benefitOrderNo(),
                request.platformBenefitOrderNo()
        );
        LoanApplyForwardData forwardData = new LoanApplyForwardData(
                uid,
                benefitOrder.benefitOrderNo(),
                platformBenefitOrderNo,
                applicationId,
                loanId,
                yuanToCent(request.amount()),
                request.term(),
                upstreamBankCardNum,
                upstreamBankCardNum,
                request.purpose(),
                request.loanReason(),
                request.basicInfo(),
                request.idInfo(),
                request.contactInfo(),
                request.supplementInfo(),
                request.optionInfo(),
                request.imageInfo()
        );
        long startNanos = System.nanoTime();
        try {
            response = yunkaCallTemplate.execute(
                    YunkaCallTemplate.YunkaCall.of(
                            "loan apply",
                            requestId,
                            yunkaProperties.paths().loanApply(),
                            applicationId,
                            forwardData
                    ).withMemberId(memberId).withBenefitOrderNo(benefitOrder.benefitOrderNo()),
                    gatewayResponse -> {
                        YunkaGatewayClient.YunkaGatewayResponse presentResponse =
                                yunkaCallTemplate.requirePresentResponse(gatewayResponse);
                        if (!yunkaCallTemplate.isSuccessful(presentResponse)) {
                            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, presentResponse.message());
                        }
                        return presentResponse;
                    }
            );
        } catch (UpstreamTimeoutException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                    ErrorCodes.YUNKA_UPSTREAM_TIMEOUT,
                    "Yunka loan apply timeout, async compensation enqueued");
            saveApplicationMapping(memberId, uid, applicationId, benefitOrder.benefitOrderNo(), loanId, request.purpose(), "PENDING_REVIEW");
            asyncCompensationEnqueueService.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                    "YUNKA_LOAN_APPLY_RETRY",
                    "LOAN_APPLY:" + applicationId,
                    applicationId,
                    "YUNKA",
                    yunkaProperties.gatewayPath(),
                    "POST",
                    null,
                    """
                    {"requestId":"%s","path":"%s","bizOrderNo":"%s","uid":"%s","benefitOrderNo":"%s","applyId":"%s","loanId":"%s","loanAmount":%d,"loanPeriod":%d,"bankCardNo":"%s"}
                    """.formatted(
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
                    ).replace("\n", "").trim()
            ));
            return new LoanApplyResponse(
                    applicationId,
                    "pending",
                    h5I18nService.text("loan.approval.arrivalTime", "30分钟"),
                    true,
                    benefitOrder.benefitOrderNo(),
                    h5I18nService.text("loan.apply.pendingReview", "借款申请已提交，正在审核中")
            );
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            return buildLoanFailedResponse(
                    applicationId,
                    benefitOrder.benefitOrderNo(),
                    (ErrorCodes.YUNKA_RESPONSE_EMPTY.equals(exception.getErrorNo())
                            || ErrorCodes.YUNKA_UPSTREAM_REJECTED.equals(exception.getErrorNo()))
                            ? exception.getErrorMsg()
                            : exception.getMessage()
            );
        } catch (RuntimeException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                    exception instanceof BizException bizException
                            ? bizException.getErrorNo()
                            : ErrorCodes.YUNKA_UPSTREAM_FAILED,
                    exception instanceof BizException bizException
                            ? bizException.getErrorMsg()
                            : exception.getMessage());
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), exception.getMessage());
        }
        String upstreamLoanId = readText(response.data(), "loanId", loanId);
        saveApplicationMapping(memberId, uid, applicationId, benefitOrder.benefitOrderNo(), upstreamLoanId, request.purpose(), "ACTIVE");
        return new LoanApplyResponse(
                applicationId,
                "pending",
                h5I18nService.text("loan.approval.arrivalTime", "30分钟"),
                true,
                benefitOrder.benefitOrderNo(),
                readRemark(response.data(), "借款申请已提交，正在处理中")
        );
    }

    private void validateApplyRequest(LoanApplyRequest request) {
        validateAmountAndTerm(request.amount(), request.term());
        String accountId = h5LoanProperties.receivingAccount().accountId();
        if (!accountId.equals(request.receivingAccountId())) {
            throw new BizException(400, "receiving account is unsupported");
        }
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

    private LoanApplyResponse buildLoanFailedResponse(String applicationId, String benefitOrderNo, String reason) {
        String safeReason = reason == null || reason.isBlank() ? "Yunka gateway response is empty" : reason;
        return new LoanApplyResponse(
                null,
                "loan_failed",
                null,
                true,
                benefitOrderNo,
                h5I18nService.text("loan.apply.failurePrefix", "权益购买成功，借款申请失败：") + safeReason
        );
    }

    private void saveApplicationMapping(
            String memberId,
            String uid,
            String applicationId,
            String benefitOrderNo,
            String loanId,
            String purpose,
            String mappingStatus
    ) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(applicationId);
        mapping.setMemberId(memberId);
        mapping.setBenefitOrderNo(benefitOrderNo);
        mapping.setChannelCode(DEFAULT_CHANNEL_CODE);
        mapping.setExternalUserId(uid);
        mapping.setUpstreamQueryType("loanId");
        mapping.setUpstreamQueryValue(loanId);
        mapping.setPurpose(purpose);
        mapping.setMappingStatus(mappingStatus);
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        loanApplicationMappingRepository.insert(mapping);
    }

    private String resolveBankCardNum(LoanApplyRequest request) {
        if (hasText(request.bankCardNum())) {
            return request.bankCardNum();
        }
        return request.receivingAccountId();
    }

    private String resolvePlatformBenefitOrderNo(String benefitOrderNo, String requestPlatformBenefitOrderNo) {
        if (hasText(requestPlatformBenefitOrderNo)) {
            return requestPlatformBenefitOrderNo;
        }
        return benefitOrderNo;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String readRemark(JsonNode data, String fallback) {
        String remark = readText(data, "remark", fallback);
        return remark.isBlank() ? fallback : remark;
    }

    private String readText(JsonNode data, String fieldName, String fallback) {
        if (data == null || data.isNull()) {
            return fallback;
        }
        String value = data.path(fieldName).asText();
        return value.isBlank() ? fallback : value;
    }

    private record LoanApplyForwardData(
            String uid,
            String benefitOrderNo,
            String platformBenefitOrderNo,
            String applyId,
            String loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo,
            String bankCardNum,
            String purpose,
            String loanReason,
            JsonNode basicInfo,
            JsonNode idInfo,
            JsonNode contactInfo,
            JsonNode supplementInfo,
            JsonNode optionInfo,
            JsonNode imageInfo
    ) {
    }
}
```

Implementation rules:

- Keep `applicationId` and `loanId` generation prefixes exactly the same.
- Keep the Yunka template call shape and rejected handling exactly the same.
- Keep timeout logging, mapping save, enqueue payload fields, and returned `pending` response exactly the same.
- Keep the existing `buildLoanFailedResponse(...)` semantics unchanged.
- Keep the raw JSON string payload for `AsyncCompensationEnqueueService.EnqueueCommand`.
- Do not move payload records to another package in this phase.

- [ ] **Step 3: Run the focused test until it passes**

Run:

```bash
mvn -Dtest=LoanApplicationServiceTest test
```

Expected result:

- `Tests run: 4`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 4: Commit the extracted application service**

```bash
git add src/main/java/com/nexusfin/equity/service/LoanApplicationService.java \
        src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java
git commit -m "refactor: extract loan application service"
```

### Task 3: Convert LoanServiceImpl into a Pure Facade

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- Test: `mvn -Dtest=LoanServiceTest,LoanApplicationServiceTest test`

- [ ] **Step 1: Add facade-level apply regression tests in LoanServiceTest before rewiring production**

Update `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java` with these changes:

- add `@Mock private LoanApplicationService loanApplicationService;`
- remove apply-behavior tests that belong in `LoanApplicationServiceTest`
- add one facade delegation test for `apply(...)`
- keep the existing calculator/query facade tests

Add this test:

```java
@Test
void shouldDelegateApplyToLoanApplicationService() throws Exception {
    LoanApplyRequest request = buildApplyRequest();
    LoanApplyResponse delegated = new LoanApplyResponse(
            "APP-001",
            "pending",
            "30分钟",
            true,
            "BEN-001",
            "借款申请已提交，正在审核中"
    );
    when(loanApplicationService.apply("mem-001", "user-001", request)).thenReturn(delegated);

    LoanApplyResponse response = loanService.apply("mem-001", "user-001", request);

    assertThat(response).isSameAs(delegated);
    verify(loanApplicationService).apply("mem-001", "user-001", request);
}
```

- [ ] **Step 2: Run the facade test first and confirm it fails because production constructor/wiring is not updated yet**

Run:

```bash
mvn -Dtest=LoanServiceTest,LoanApplicationServiceTest test
```

Expected red phase:

- `constructor LoanServiceImpl ... cannot be applied to given types`
- or `apply(...)` still executes old inline logic instead of delegation

- [ ] **Step 3: Rewire LoanServiceImpl to delegate and remove apply-only state**

Update `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java` into a pure facade with this structure:

```java
package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.service.LoanApplicationService;
import com.nexusfin.equity.service.LoanApprovalQueryService;
import com.nexusfin.equity.service.LoanCalculatorService;
import com.nexusfin.equity.service.LoanService;
import org.springframework.stereotype.Service;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanCalculatorService loanCalculatorService;
    private final LoanApplicationService loanApplicationService;
    private final LoanApprovalQueryService loanApprovalQueryService;

    public LoanServiceImpl(
            LoanCalculatorService loanCalculatorService,
            LoanApplicationService loanApplicationService,
            LoanApprovalQueryService loanApprovalQueryService
    ) {
        this.loanCalculatorService = loanCalculatorService;
        this.loanApplicationService = loanApplicationService;
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

    @Override
    public LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request) {
        return loanApplicationService.apply(memberId, uid, request);
    }

    @Override
    public LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId) {
        return loanApprovalQueryService.getApprovalStatus(memberId, applicationId);
    }

    @Override
    public LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId) {
        return loanApprovalQueryService.getApprovalResult(memberId, applicationId);
    }
}
```

Important cleanup in this step:

- remove all old direct fields and imports from `LoanServiceImpl`
- remove `Logger`, `TraceIdUtil`, `@Transactional`, repository, benefit order, enqueue, i18n, Yunka, and helper imports from `LoanServiceImpl`
- delete these moved members from `LoanServiceImpl`:
  - `validateApplyRequest(...)`
  - `validateAmountAndTerm(...)`
  - `buildLoanFailedResponse(...)`
  - `saveApplicationMapping(...)`
  - `resolveBankCardNum(...)`
  - `resolvePlatformBenefitOrderNo(...)`
  - `hasText(...)`
  - `readRemark(...)`
  - `readText(...)`
  - `LoanApplyForwardData`

- [ ] **Step 4: Simplify LoanServiceTest to match the pure facade constructor**

In `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`:

- instantiate `LoanServiceImpl` with only:
  - `loanCalculatorService`
  - `loanApplicationService`
  - `loanApprovalQueryService`
- remove now-unused mocks:
  - `YunkaGatewayClient`
  - `LoanApplicationMappingRepository`
  - `BenefitOrderService`
  - `H5I18nService`
  - `AsyncCompensationEnqueueService`
  - `XiaohuaGatewayService`
- remove `OutputCaptureExtension`
- remove now-unused helper builders for `H5LoanProperties`, `H5BenefitsProperties`, `YunkaProperties`
- keep only the minimal `buildApplyRequest()` helper needed for the facade apply test

- [ ] **Step 5: Run the focused regression until it passes**

Run:

```bash
mvn -Dtest=LoanServiceTest,LoanApplicationServiceTest test
```

Expected result:

- `BUILD SUCCESS`
- `Failures: 0`
- `Errors: 0`

- [ ] **Step 6: Commit the facade delegation**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
        src/test/java/com/nexusfin/equity/service/LoanServiceTest.java
git commit -m "refactor: delegate loan application facade to extracted service"
```

### Task 4: Final Focused Verification and Structure Check

**Files:**
- Verify only, no intended code change
- Test: `mvn -Dtest=LoanServiceTest,LoanApplicationServiceTest test`
- Test: `mvn -q -DskipTests compile`

- [ ] **Step 1: Run the focused service regression suite**

```bash
mvn -Dtest=LoanServiceTest,LoanApplicationServiceTest test
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

- [ ] **Step 3: Confirm apply-only helpers no longer live in LoanServiceImpl**

```bash
rg -n "private void validateApplyRequest|private void validateAmountAndTerm|private LoanApplyResponse buildLoanFailedResponse|private void saveApplicationMapping|private String resolveBankCardNum|private String resolvePlatformBenefitOrderNo|private record LoanApplyForwardData" \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java
```

Expected:

- no matches
- exit code `1`

- [ ] **Step 4: Confirm Phase 2C stayed within scope**

Run:

```bash
git show --name-only --format='' HEAD~1..HEAD
```

Verify the phase only touched:

- `src/main/java/com/nexusfin/equity/service/LoanApplicationService.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/LoanApplicationServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`

No new edits should appear in:

- `src/main/java/com/nexusfin/equity/service/impl/LoanCalculatorServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/LoanApprovalQueryServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`

---

## Self-Review

- Spec coverage checked: this plan covers only the `LoanApplicationService` line item from Phase 2 and intentionally leaves `LoanApplicationGateway`, payload relocation, typed enqueue payload, and repayment-side splitting for later phases.
- Placeholder scan checked: the plan contains exact file paths, commands, and concrete code skeletons; no `TODO` or `TBD` markers remain.
- Type consistency checked: constructor changes, DTO names, and service method signatures align with the current `LoanService` contract and with the already-completed calculator/query split pattern.
