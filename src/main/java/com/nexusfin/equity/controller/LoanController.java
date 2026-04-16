package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.LoanService;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.AuthPrincipal;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/loan")
public class LoanController {

    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/calculator-config")
    public Result<LoanCalculatorConfigResponse> getCalculatorConfig() {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} loan calculator config requested by memberId={}",
                TraceIdUtil.getTraceId(), "loan-calculator-config", principal.memberId());
        return Result.success(loanService.getCalculatorConfig());
    }

    @PostMapping("/calculate")
    public Result<LoanCalculateResponse> calculate(@Valid @RequestBody LoanCalculateRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} loan calculate requested by memberId={}",
                TraceIdUtil.getTraceId(), "loan-calculate", principal.memberId());
        return Result.success(loanService.calculate(principal.memberId(), principal.techPlatformUserId(), request));
    }


    @PostMapping("/apply")
    public Result<LoanApplyResponse> apply(@Valid @RequestBody LoanApplyRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} loan apply requested by memberId={}",
                TraceIdUtil.getTraceId(), "loan-apply", principal.memberId());
        return Result.success(loanService.apply(principal.memberId(), principal.techPlatformUserId(), request));
    }

    @GetMapping("/approval-status/{applicationId}")
    public Result<LoanApprovalStatusResponse> getApprovalStatus(@PathVariable String applicationId) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} approval status requested by memberId={}",
                TraceIdUtil.getTraceId(), applicationId, principal.memberId());
        return Result.success(loanService.getApprovalStatus(principal.memberId(), applicationId));
    }

    @GetMapping("/approval-result/{applicationId}")
    public Result<LoanApprovalResultResponse> getApprovalResult(@PathVariable String applicationId) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} approval result requested by memberId={}",
                TraceIdUtil.getTraceId(), applicationId, principal.memberId());
        return Result.success(loanService.getApprovalResult(principal.memberId(), applicationId));
    }
}
