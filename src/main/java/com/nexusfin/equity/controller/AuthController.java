package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.response.CurrentUserResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.AuthService;
import com.nexusfin.equity.util.CookieUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    public AuthController(AuthService authService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
    }

    @GetMapping("/api/auth/sso-callback")
    public ResponseEntity<Void> ssoCallback(
            @RequestParam("token") String token,
            @RequestParam(value = "redirect_url", required = false) String redirectUrl
    ) {
        AuthService.AuthLoginResult loginResult = authService.loginWithTechToken(token, redirectUrl);
        log.info("traceId={} bizOrderNo={} sso callback login succeeded",
                TraceIdUtil.getTraceId(), loginResult.redirectUrl());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(loginResult.redirectUrl()))
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildAuthCookie(loginResult.jwtToken()).toString())
                .build();
    }

    @GetMapping("/api/users/me")
    public Result<CurrentUserResponse> getCurrentUser() {
        CurrentUserResponse response = authService.getCurrentUser();
        log.info("traceId={} bizOrderNo={} current user resolved",
                TraceIdUtil.getTraceId(), response.memberId());
        return Result.success(response);
    }
}
