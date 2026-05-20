package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.impl.LoanCalculatorServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanCalculatorServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private MemberReceivingAccountService memberReceivingAccountService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private YunkaCallTemplate yunkaCallTemplate;

    private LoanCalculatorService loanCalculatorService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        loanCalculatorService = new LoanCalculatorServiceImpl(
                h5LoanProperties(),
                h5BenefitsProperties(),
                yunkaProperties(),
                h5I18nService,
                memberReceivingAccountService,
                xiaohuaGatewayService,
                yunkaCallTemplate
        );
    }

    @Test
    void shouldBuildCalculatorConfigFromFirstUserCardAndCacheAllCards() {
        when(xiaohuaGatewayService.queryUserCards(any(), any(), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-first-001", "第一银行", "1111", 0),
                        new UserCardSummary("card-second-002", "第二银行", "2222", 1)
                )));

        LoanCalculatorConfigResponse response = loanCalculatorService.getCalculatorConfig("mem-test-001");

        assertThat(response.amountRange().min()).isEqualTo(100L);
        assertThat(response.amountRange().defaultAmount()).isEqualTo(3000L);
        assertThat(response.termOptions())
                .extracting(LoanCalculatorConfigResponse.TermOption::value)
                .containsExactly(3, 6);
        assertThat(response.annualRate()).isEqualByComparingTo("0.18");
        assertThat(response.lender()).isEqualTo("XX商业银行");
        assertThat(response.orderAmount()).isEqualTo(300L);
        assertThat(response.receivingAccount().bankName()).isEqualTo("第一银行");
        assertThat(response.receivingAccount().lastFour()).isEqualTo("1111");
        assertThat(response.receivingAccount().accountId()).isEqualTo("card-first-001");

        ArgumentCaptor<UserCardListRequest> cardRequestCaptor = ArgumentCaptor.forClass(UserCardListRequest.class);
        verify(xiaohuaGatewayService).queryUserCards(any(), eq("calculator-config"), cardRequestCaptor.capture());
        assertThat(cardRequestCaptor.getValue().userId()).isEqualTo("mem-test-001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MemberReceivingAccountService.CardCacheCommand>> cacheCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(memberReceivingAccountService).cacheReceivingAccounts(eq("mem-test-001"), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue())
                .extracting(
                        MemberReceivingAccountService.CardCacheCommand::accountId,
                        MemberReceivingAccountService.CardCacheCommand::sourceIndex
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("card-first-001", 0),
                        org.assertj.core.groups.Tuple.tuple("card-second-002", 1)
                );
    }

    @Test
    void shouldReturnBindCardRequiredConfigWhenUserCardListIsEmpty() {
        when(xiaohuaGatewayService.queryUserCards(any(), any(), any()))
                .thenReturn(new UserCardListResponse(List.of()));

        LoanCalculatorConfigResponse response = loanCalculatorService.getCalculatorConfig("mem-test-001");

        assertThat(response.amountRange().min()).isEqualTo(100L);
        assertThat(response.termOptions())
                .extracting(LoanCalculatorConfigResponse.TermOption::value)
                .containsExactly(3, 6);
        assertThat(response.orderAmount()).isEqualTo(300L);
        assertThat(response.receivingAccount()).isNull();
        assertThat(response.bindCardRequired()).isTrue();
        assertThat(response.bindCardMessage()).isEqualTo("请到科技平台绑卡后重试");
    }

    @Test
    void shouldCalculateRepaymentPlanFromYunkaTrailResponse() throws Exception {
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "receiveAmount": 3000.00,
                          "repayAmount": 3123.45,
                          "yearRate": 18.0,
                          "repayPlan": [
                            {
                              "period": 1,
                              "date": "2026-05-07",
                              "principal": 1000.00,
                              "interest": 45.00,
                              "total": 1045.00
                            },
                            {
                              "period": 2,
                              "date": "2026-06-07",
                              "principal": 1000.00,
                              "interest": 40.00,
                              "total": 1040.00
                            }
                          ]
                        }
                        """));

        LoanCalculateResponse response = loanCalculatorService.calculate(
                "mem-test-001",
                "cid-test-001",
                new LoanCalculateRequest(3000L, 3)
        );

        assertThat(response.totalFee()).isEqualByComparingTo("123.45");
        assertThat(response.annualRate()).isEqualTo("18.0%");
        assertThat(response.repaymentPlan()).hasSize(2);
        assertThat(response.repaymentPlan().get(0).total()).isEqualByComparingTo("1045.00");

        ArgumentCaptor<YunkaCallTemplate.YunkaCall> captor = ArgumentCaptor.forClass(YunkaCallTemplate.YunkaCall.class);
        verify(yunkaCallTemplate).executeForData(captor.capture());
        assertThat(captor.getValue().scene()).isEqualTo("loan calculate");
        assertThat(captor.getValue().memberId()).isEqualTo("mem-test-001");
        assertThat(captor.getValue().path()).isEqualTo("/loan/trial");
        var payload = objectMapper.valueToTree(captor.getValue().payload());
        assertThat(payload.path("loanAmount").decimalValue())
                .isEqualByComparingTo("3000.00");
        assertThat(payload.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(payload.path("userId").asText()).isNotEqualTo("cid-test-001");
        assertThat(payload.has("uid")).isFalse();
        assertThat(payload.has("applyId")).isFalse();
    }

    @Test
    void shouldMapCurrentYunkaTrialResponseFieldsForCalculatorDisplay() throws Exception {
        when(yunkaCallTemplate.executeForData(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "status": "SUCCESS",
                          "providerCode": "0",
                          "providerMessage": "success",
                          "retryable": false,
                          "originalRefund": 3000.0,
                          "receiveAmount": 3120.75,
                          "repayAmount": 3120.75,
                          "yearRate": 24.0,
                          "repayPlan": [
                            {
                              "dueTime": 1781280000000,
                              "originalRepay": 1040.25,
                              "periodNo": 1,
                              "repayAmount": 1040.25,
                              "repayGuaranteeFee": 0.0,
                              "repayInterest": 21.25,
                              "repayOtherCharge": 0.0,
                              "repayPrincipal": 992.95,
                              "repaySvcFee": 26.05
                            }
                          ]
                        }
                        """));

        LoanCalculateResponse response = loanCalculatorService.calculate(
                "mem-test-001",
                "cid-test-001",
                new LoanCalculateRequest(3000L, 3)
        );

        assertThat(response.totalFee()).isEqualByComparingTo("120.75");
        assertThat(response.annualRate()).isEqualTo("24.0%");
        assertThat(response.repaymentPlan()).hasSize(1);
        LoanCalculateResponse.RepaymentPlanItem firstPeriod = response.repaymentPlan().get(0);
        assertThat(firstPeriod.period()).isEqualTo(1);
        assertThat(firstPeriod.date()).isEqualTo("2026-06-13");
        assertThat(firstPeriod.principal()).isEqualByComparingTo("992.95");
        assertThat(firstPeriod.interest()).isEqualByComparingTo("21.25");
        assertThat(firstPeriod.total()).isEqualByComparingTo("1040.25");
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
                "mem-test-001",
                "cid-test-001",
                new LoanCalculateRequest(3000L, 3)
        );

        assertThat(response.totalFee()).isEqualByComparingTo("0.00");
        assertThat(response.annualRate()).isEqualTo("18.0%");
        assertThat(response.repaymentPlan()).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedAmountOrTermBeforeCallingYunka() {
        assertThatThrownBy(() -> loanCalculatorService.calculate(
                "mem-test-001",
                "cid-test-001",
                new LoanCalculateRequest(3050L, 3)
        ))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("amount step is invalid");

        assertThatThrownBy(() -> loanCalculatorService.calculate(
                "mem-test-001",
                "cid-test-001",
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
                "XX商业银行"
        );
    }

    private H5BenefitsProperties h5BenefitsProperties() {
        return new H5BenefitsProperties(
                "HUXUAN_CARD",
                true,
                false,
                new H5BenefitsProperties.Activate(300000L, 30000L, "huixuan_card", "惠选卡开通成功"),
                new H5BenefitsProperties.Detail(
                        "惠选卡",
                        300L,
                        448L,
                        List.of(),
                        List.of(),
                        List.of(),
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
                        "/loan/trial",
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
                        "/huijuapi/vip/orderNotice"
                ),
                "ABS",
                "ABS-YUNKA-TEST",
                "yunka-test-secret"
        );
    }
}
