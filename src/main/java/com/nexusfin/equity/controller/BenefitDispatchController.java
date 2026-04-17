package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.BenefitDispatchResolveRequest;
import com.nexusfin.equity.dto.response.BenefitDispatchContextResponse;
import com.nexusfin.equity.dto.response.BenefitDispatchResolveResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.BenefitDispatchService;
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
@RequestMapping("/api/benefit-dispatch")
public class BenefitDispatchController {

    private static final Logger log = LoggerFactory.getLogger(BenefitDispatchController.class);

    private final BenefitDispatchService benefitDispatchService;

    public BenefitDispatchController(BenefitDispatchService benefitDispatchService) {
        this.benefitDispatchService = benefitDispatchService;
    }

    @GetMapping("/context/{benefitOrderNo}")
    public Result<BenefitDispatchContextResponse> getContext(@PathVariable String benefitOrderNo) {
        log.info("traceId={} bizOrderNo={} benefit dispatch context requested",
                TraceIdUtil.getTraceId(), benefitOrderNo);
        return Result.success(benefitDispatchService.getContext(benefitOrderNo));
    }

    @PostMapping("/resolve")
    public Result<BenefitDispatchResolveResponse> resolve(@Valid @RequestBody BenefitDispatchResolveRequest request) {
        log.info("traceId={} bizOrderNo={} benefit dispatch target resolve requested",
                TraceIdUtil.getTraceId(), request.benefitOrderNo());
        return Result.success(benefitDispatchService.resolve(request.benefitOrderNo()));
    }
}
