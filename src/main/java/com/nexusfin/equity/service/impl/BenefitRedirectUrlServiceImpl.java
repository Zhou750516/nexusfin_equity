package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.service.BenefitRedirectUrlService;
import com.nexusfin.equity.service.JointLoginService;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlRequest;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
import com.nexusfin.equity.util.TraceIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BenefitRedirectUrlServiceImpl implements BenefitRedirectUrlService {

    private static final Logger log = LoggerFactory.getLogger(BenefitRedirectUrlServiceImpl.class);

    private final JointLoginService jointLoginService;
    private final QwBenefitClient qwBenefitClient;

    public BenefitRedirectUrlServiceImpl(
            JointLoginService jointLoginService,
            QwBenefitClient qwBenefitClient
    ) {
        this.jointLoginService = jointLoginService;
        this.qwBenefitClient = qwBenefitClient;
    }

    @Override
    public BenefitRedirectUrlResult generate(BenefitRedirectUrlRequest request) {
        JointLoginService.JointLoginResult loginResult;
        try {
            loginResult = jointLoginService.login(new com.nexusfin.equity.dto.request.JointLoginRequest(
                    request.token(),
                    "exercise",
                    null,
                    request.benefitOrderNo(),
                    null
            ));
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} benefit redirect joint login failed errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.benefitOrderNo(),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw exception;
        }

        try {
            // Current business assumption: benefit redirect URL is satisfied by the QW exercise redirect URL.
            // This preserves the external redrect_benefit_url contract while keeping the actual upstream
            // dependency explicit for later protocol clarification.
            QwExerciseUrlResponse response = qwBenefitClient.getExerciseUrl(new QwExerciseUrlRequest(
                    loginResult.externalUserId(),
                    request.benefitOrderNo()
            ));
            return new BenefitRedirectUrlResult(response.redirectUrl());
        } catch (UpstreamTimeoutException exception) {
            log.error("traceId={} bizOrderNo={} benefit redirect qw redirect url timed out errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.benefitOrderNo(),
                    exception.getMessage());
            throw new BizException("REDRECT_BENEFIT_URL_UPSTREAM_TIMEOUT", "Benefit redirect url temporarily unavailable");
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} benefit redirect qw redirect url failed errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.benefitOrderNo(),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw new BizException("REDRECT_BENEFIT_URL_UPSTREAM_FAILED", "Benefit redirect url is unavailable");
        }
    }
}
