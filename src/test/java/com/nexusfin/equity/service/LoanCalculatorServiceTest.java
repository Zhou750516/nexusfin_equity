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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanCalculatorServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private LoanReceivingAccountService loanReceivingAccountService;

    @Mock
    private YunkaCallTemplate yunkaCallTemplate;

    private LoanCalculatorService loanCalculatorService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        loanCalculatorService = new LoanCalculatorServiceImpl(
                h5LoanProperties(),
                yunkaProperties(),
                h5I18nService,
                loanReceivingAccountService,
                yunkaCallTemplate
        );
    }

    @Test
    void shouldBuildCalculatorConfigFromDatabaseReceivingAccount() {
        when(loanReceivingAccountService.getDefaultReceivingAccount())
                .thenReturn(new LoanReceivingAccountService.ReceivingAccountDetails("acc-db-001", "测试银行", "1234"));

        LoanCalculatorConfigResponse response = loanCalculatorService.getCalculatorConfig();

        assertThat(response.amountRange().min()).isEqualTo(100L);
        assertThat(response.amountRange().defaultAmount()).isEqualTo(3000L);
        assertThat(response.termOptions())
                .extracting(LoanCalculatorConfigResponse.TermOption::value)
                .containsExactly(3, 6);
        assertThat(response.annualRate()).isEqualByComparingTo("0.18");
        assertThat(response.lender()).isEqualTo("XX商业银行");
        assertThat(response.receivingAccount().bankName()).isEqualTo("测试银行");
        assertThat(response.receivingAccount().lastFour()).isEqualTo("1234");
        assertThat(response.receivingAccount().accountId()).isEqualTo("acc-db-001");
    }

    @Test
    void shouldFailWhenDatabaseReceivingAccountIsMissing() {
        when(loanReceivingAccountService.getDefaultReceivingAccount())
                .thenThrow(new BizException("LOAN_RECEIVING_ACCOUNT_NOT_CONFIGURED",
                        "Default loan receiving account is not configured"));

        assertThatThrownBy(() -> loanCalculatorService.getCalculatorConfig())
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorNo())
                .isEqualTo("LOAN_RECEIVING_ACCOUNT_NOT_CONFIGURED");
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
                        "/benefit/sync"
                ),
                "ABS",
                "ABS-YUNKA-TEST",
                "yunka-test-secret"
        );
    }
}
