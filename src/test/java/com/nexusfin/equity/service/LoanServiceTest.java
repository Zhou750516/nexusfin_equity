package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.impl.LoanServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LoanServiceTest {

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

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private LoanApprovalQueryService loanApprovalQueryService;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
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
                new BigDecimal("3000.00"),
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

    @Test
    void shouldForwardRichApplyFieldsWhileKeepingCurrentPayloadCompatible() throws Exception {
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

        LoanApplyRequest request = buildApplyRequest();

        LoanApplyResponse response = loanService.apply("mem-001", "user-001", request);

        assertThat(response.applicationId()).startsWith("APP-");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        JsonNode forwardData = objectMapper.valueToTree(captor.getValue().data());
        assertThat(forwardData.path("purpose").asText()).isEqualTo("rent");
        assertThat(forwardData.path("loanReason").asText()).isEqualTo("DAILY_CONSUMPTION");
        assertThat(forwardData.path("bankCardNum").asText()).isEqualTo("6222020202028648");
        assertThat(forwardData.path("platformBenefitOrderNo").asText()).isEqualTo("PBEN-001");
        assertThat(forwardData.path("basicInfo").path("education").asText()).isEqualTo("BACHELOR");
        assertThat(forwardData.path("contactInfo").isArray()).isTrue();
        assertThat(forwardData.path("imageInfo").isArray()).isTrue();
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

        LoanApplyResponse response = loanService.apply("mem-001", "user-001", buildApplyRequest());

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
    void shouldEnqueueCompensationWhenLoanApplyTimesOut() throws Exception {
        when(benefitOrderService.createOrder(eq("mem-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-TIMEOUT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenThrow(new UpstreamTimeoutException("Yunka gateway timeout"));

        LoanApplyResponse response = loanService.apply("mem-001", "user-001", buildApplyRequest());

        assertThat(response.status()).isEqualTo("pending");
        verify(asyncCompensationEnqueueService).enqueue(any());
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
