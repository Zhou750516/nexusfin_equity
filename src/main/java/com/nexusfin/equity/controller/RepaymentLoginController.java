package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.RepaymentLoginRequest;
import com.nexusfin.equity.dto.response.RepaymentLoginResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.RepaymentLoginService;
import com.nexusfin.equity.util.CookieUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class RepaymentLoginController {

    private static final Logger log = LoggerFactory.getLogger(RepaymentLoginController.class);

    private final RepaymentLoginService repaymentLoginService;
    private final CookieUtil cookieUtil;

    public RepaymentLoginController(
            RepaymentLoginService repaymentLoginService,
            CookieUtil cookieUtil
    ) {
        this.repaymentLoginService = repaymentLoginService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/api/auth/repayment-login")
    public ResponseEntity<Result<RepaymentLoginResponse>> repaymentLogin(
            @Valid @RequestBody RepaymentLoginRequest request
    ) {
        RepaymentLoginService.RepaymentLoginResult result = repaymentLoginService.login(request);
        log.info("traceId={} bizOrderNo={} tokenPresent={} repayment login cookie issued",
                TraceIdUtil.getTraceId(),
                result.loanId(),
                true);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildAuthCookie(result.jwtToken()).toString())
                .body(Result.success(new RepaymentLoginResponse(true, result.loanId())));
    }
}
