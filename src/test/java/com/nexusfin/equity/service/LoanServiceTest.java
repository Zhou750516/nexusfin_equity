package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.service.impl.LoanServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanApplicationService loanApplicationService;

    @Mock
    private LoanApprovalQueryService loanApprovalQueryService;

    @Mock
    private LoanCalculatorService loanCalculatorService;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanServiceImpl(
                loanCalculatorService,
                loanApplicationService,
                loanApprovalQueryService
        );
    }

    @Test
    void shouldDelegateCalculatorConfigToLoanCalculatorService() {
        LoanCalculatorConfigResponse delegated = new LoanCalculatorConfigResponse(
                new LoanCalculatorConfigResponse.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new LoanCalculatorConfigResponse.TermOption("3期", 3)),
                new BigDecimal("0.18"),
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

    @Test
    void shouldDelegateApplyToLoanApplicationService() {
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

    private LoanApplyRequest buildApplyRequest() {
        return new LoanApplyRequest(
                3000L,
                3,
                "acc_001",
                List.of("loan", "user"),
                "rent",
                "DAILY_CONSUMPTION",
                "6222020202028648",
                null,
                null,
                null,
                null,
                null,
                null,
                "PBEN-001"
        );
    }
}
