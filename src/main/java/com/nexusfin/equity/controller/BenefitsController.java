package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.BenefitsService;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/benefits")
public class BenefitsController {

    private static final Logger log = LoggerFactory.getLogger(BenefitsController.class);

    private final BenefitsService benefitsService;

    public BenefitsController(BenefitsService benefitsService) {
        this.benefitsService = benefitsService;
    }

    @GetMapping("/card-detail")
    public Result<BenefitsCardDetailResponse> getCardDetail() {
        String memberId = AuthContextUtil.getRequiredPrincipal().memberId();
        log.info("traceId={} bizOrderNo={} benefits card detail requested by memberId={}",
                TraceIdUtil.getTraceId(), "benefits-card-detail", memberId);
        return Result.success(benefitsService.getCardDetail());
    }

    @PostMapping("/activate")
    public Result<BenefitsActivateResponse> activate(@Valid @RequestBody BenefitsActivateRequest request) {
        String memberId = AuthContextUtil.getRequiredPrincipal().memberId();
        BenefitsActivateResponse response = benefitsService.activate(memberId, request);
        log.info("traceId={} bizOrderNo={} benefits activated by memberId={}",
                TraceIdUtil.getTraceId(), response.activationId(), memberId);
        return Result.success(response);
    }
}
