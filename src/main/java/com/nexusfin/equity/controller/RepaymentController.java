package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.RepaymentSmsConfirmRequest;
import com.nexusfin.equity.dto.request.RepaymentSmsSendRequest;
import com.nexusfin.equity.dto.request.RepaymentSubmitRequest;
import com.nexusfin.equity.dto.response.RepaymentInfoResponse;
import com.nexusfin.equity.dto.response.RepaymentResultResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsConfirmResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsSendResponse;
import com.nexusfin.equity.dto.response.RepaymentSubmitResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.RepaymentService;
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
@RequestMapping("/api/repayment")
public class RepaymentController {

    private static final Logger log = LoggerFactory.getLogger(RepaymentController.class);

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @GetMapping("/info/{loanId}")
    public Result<RepaymentInfoResponse> getInfo(@PathVariable String loanId) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} repayment info requested by memberId={}",
                TraceIdUtil.getTraceId(), loanId, principal.memberId());
        return Result.success(repaymentService.getInfo(principal.techPlatformUserId(), loanId));
    }

    @PostMapping("/sms-send")
    public Result<RepaymentSmsSendResponse> sendSms(@Valid @RequestBody RepaymentSmsSendRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} repayment sms send requested by memberId={}",
                TraceIdUtil.getTraceId(), request.loanId(), principal.memberId());
        return Result.success(repaymentService.sendSms(principal.techPlatformUserId(), request));
    }

    @PostMapping("/sms-confirm")
    public Result<RepaymentSmsConfirmResponse> confirmSms(@Valid @RequestBody RepaymentSmsConfirmRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} repayment sms confirm requested by memberId={}",
                TraceIdUtil.getTraceId(), request.loanId(), principal.memberId());
        return Result.success(repaymentService.confirmSms(principal.techPlatformUserId(), request));
    }

    @PostMapping("/submit")
    public Result<RepaymentSubmitResponse> submit(@Valid @RequestBody RepaymentSubmitRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} repayment submit requested by memberId={}",
                TraceIdUtil.getTraceId(), request.loanId(), principal.memberId());
        return Result.success(repaymentService.submit(principal.techPlatformUserId(), request));
    }

    @GetMapping("/result/{repaymentId}")
    public Result<RepaymentResultResponse> getResult(@PathVariable String repaymentId) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} repayment result requested by memberId={}",
                TraceIdUtil.getTraceId(), repaymentId, principal.memberId());
        return Result.success(repaymentService.getResult(principal.techPlatformUserId(), repaymentId));
    }
}
