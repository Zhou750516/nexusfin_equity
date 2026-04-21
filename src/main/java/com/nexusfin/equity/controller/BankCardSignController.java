package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.BankCardSignApplyRequest;
import com.nexusfin.equity.dto.request.BankCardSignConfirmRequest;
import com.nexusfin.equity.dto.response.BankCardSignApplyResponse;
import com.nexusfin.equity.dto.response.BankCardSignConfirmResponse;
import com.nexusfin.equity.dto.response.BankCardSignStatusResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.BankCardSignService;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.AuthPrincipal;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/bank-card")
public class BankCardSignController {

    private static final Logger log = LoggerFactory.getLogger(BankCardSignController.class);

    private final BankCardSignService bankCardSignService;

    public BankCardSignController(BankCardSignService bankCardSignService) {
        this.bankCardSignService = bankCardSignService;
    }

    @GetMapping("/sign-status")
    public Result<BankCardSignStatusResponse> getSignStatus(@RequestParam @NotBlank String accountNo) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} bank-card sign status requested by memberId={}",
                TraceIdUtil.getTraceId(), bankCardBizOrderNo(accountNo), principal.memberId());
        return Result.success(bankCardSignService.getSignStatus(principal.memberId(), accountNo));
    }

    @PostMapping("/sign-apply")
    public Result<BankCardSignApplyResponse> applySign(@Valid @RequestBody BankCardSignApplyRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} bank-card sign apply requested by memberId={}",
                TraceIdUtil.getTraceId(), bankCardBizOrderNo(request.accountNo()), principal.memberId());
        return Result.success(bankCardSignService.applySign(principal.memberId(), request));
    }

    @PostMapping("/sign-confirm")
    public Result<BankCardSignConfirmResponse> confirmSign(@Valid @RequestBody BankCardSignConfirmRequest request) {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        log.info("traceId={} bizOrderNo={} bank-card sign confirm requested by memberId={}",
                TraceIdUtil.getTraceId(), bankCardBizOrderNo(request.accountNo()), principal.memberId());
        return Result.success(bankCardSignService.confirmSign(principal.memberId(), request));
    }

    private String bankCardBizOrderNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return "bank-card-UNKNOWN";
        }
        return "bank-card-" + (accountNo.length() <= 4 ? accountNo : accountNo.substring(accountNo.length() - 4));
    }
}
