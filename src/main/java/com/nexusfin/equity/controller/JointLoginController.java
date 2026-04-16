package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.dto.response.JointLoginResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.JointLoginService;
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
public class JointLoginController {

    private static final Logger log = LoggerFactory.getLogger(JointLoginController.class);

    private final JointLoginService jointLoginService;
    private final CookieUtil cookieUtil;

    public JointLoginController(
            JointLoginService jointLoginService,
            CookieUtil cookieUtil
    ) {
        this.jointLoginService = jointLoginService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/api/auth/joint-login")
    public ResponseEntity<Result<JointLoginResponse>> jointLogin(
            @Valid @RequestBody JointLoginRequest request
    ) {
        JointLoginService.JointLoginResult result = jointLoginService.login(request);
        JointLoginResponse response = new JointLoginResponse(
                true,
                result.scene(),
                result.targetPage(),
                result.benefitOrderNo(),
                result.localUserReady()
        );
        log.info("traceId={} bizOrderNo={} joint login entry resolved targetPage={}",
                TraceIdUtil.getTraceId(), request.benefitOrderNo(), result.targetPage());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildAuthCookie(result.jwtToken()).toString())
                .body(Result.success(response));
    }
}
