package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApprovalQueryServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LoanApplicationGateway loanApplicationGateway;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private YunkaCallTemplate yunkaCallTemplate;

    private LoanApprovalQueryService loanApprovalQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        loanApprovalQueryService = new LoanApprovalQueryServiceImpl(
                h5BenefitsProperties(),
                yunkaProperties(),
                loanApplicationGateway,
                h5I18nService,
                xiaohuaGatewayService,
                yunkaCallTemplate
        );
    }

    @Test
    void shouldBuildRejectedApprovalStatusWithBenefitsPreview() {
        when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-003"))
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
        when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-001"))
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
        when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-002"))
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
        when(loanApplicationGateway.findActiveOrPendingMapping("mem-001", "APP-404")).thenReturn(null);

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
