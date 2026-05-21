package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;
import com.nexusfin.equity.dto.response.BenefitRedirectUrlResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.BenefitRedirectUrlService;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class BenefitRedirectUrlController {

    private static final Logger log = LoggerFactory.getLogger(BenefitRedirectUrlController.class);

    private final BenefitRedirectUrlService benefitRedirectUrlService;

    public BenefitRedirectUrlController(BenefitRedirectUrlService benefitRedirectUrlService) {
        this.benefitRedirectUrlService = benefitRedirectUrlService;
    }

    @PostMapping("/api/auth/redrect_benefit_url")
    public Result<BenefitRedirectUrlResponse> redrectBenefitUrl(
            @Valid @RequestBody BenefitRedirectUrlRequest request
    ) {
        BenefitRedirectUrlService.BenefitRedirectUrlResult result = benefitRedirectUrlService.generate(request);
        log.info("traceId={} bizOrderNo={} benefit redirect url generated from current qw exercise runtime source",
                TraceIdUtil.getTraceId(),
                request.benefitOrderNo());
        return Result.success(new BenefitRedirectUrlResponse(result.redirectUrl()));
    }

    @GetMapping("/api/auth/redrect_benefit_url")
    public ResponseEntity<Void> redrectBenefitUrl(
            @RequestParam String benefitOrderNo
    ) {
        String memberId = AuthContextUtil.getRequiredPrincipal().memberId();
        BenefitRedirectUrlService.BenefitRedirectUrlResult result =
                benefitRedirectUrlService.generateForMember(memberId, benefitOrderNo);
        log.info("traceId={} bizOrderNo={} benefit redirect url generated for authenticated member",
                TraceIdUtil.getTraceId(),
                benefitOrderNo);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.redirectUrl()))
                .build();
    }
}
