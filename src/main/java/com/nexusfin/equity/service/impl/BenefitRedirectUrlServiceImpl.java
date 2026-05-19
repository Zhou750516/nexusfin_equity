package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.service.BenefitRedirectUrlService;
import com.nexusfin.equity.service.JointLoginService;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlRequest;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
import com.nexusfin.equity.util.ErrorLogFields;
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
        // Runtime truth for the current redrect_benefit_url contract:
        // 1. complete joint-login with exercise scene context
        // 2. fetch the QW exercise redirect URL
        // 3. return that URL as the current benefiturl source for benefit sync
        // This is intentionally narrower than a generic "all benefit scenarios" redirect abstraction.
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
            // Current runtime source is the QW exercise redirect URL, not a fully generalized benefit redirect.
            QwExerciseUrlResponse response = qwBenefitClient.getExerciseUrl(new QwExerciseUrlRequest(
                    loginResult.externalUserId(),
                    request.benefitOrderNo()
            ));
            log.info("traceId={} bizOrderNo={} benefit redirect url resolved from qw exercise redirect url",
                    TraceIdUtil.getTraceId(),
                    request.benefitOrderNo());
            return new BenefitRedirectUrlResult(response.redirectUrl());
        } catch (UpstreamTimeoutException exception) {
            log.error("traceId={} bizOrderNo={} benefit redirect qw exercise redirect url timed out errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.benefitOrderNo(),
                    "REDRECT_BENEFIT_URL_UPSTREAM_TIMEOUT",
                    ErrorLogFields.errorMsg(exception, "Benefit redirect url temporarily unavailable"));
            throw new BizException("REDRECT_BENEFIT_URL_UPSTREAM_TIMEOUT", "Benefit redirect url temporarily unavailable");
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} benefit redirect qw exercise redirect url failed errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.benefitOrderNo(),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw new BizException("REDRECT_BENEFIT_URL_UPSTREAM_FAILED", "Benefit redirect url is unavailable");
        }
    }
}
