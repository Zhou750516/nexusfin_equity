package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.RefundApplyRequest;
import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.RefundService;
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
@RequestMapping("/api/refund")
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/info/{benefitOrderNo}")
    public Result<RefundInfoResponse> getInfo(@PathVariable String benefitOrderNo) {
        log.info("traceId={} bizOrderNo={} refund info requested",
                TraceIdUtil.getTraceId(), benefitOrderNo);
        return Result.success(refundService.getInfo(benefitOrderNo));
    }

    @PostMapping("/apply")
    public Result<RefundApplyResponse> apply(@Valid @RequestBody RefundApplyRequest request) {
        log.info("traceId={} bizOrderNo={} refund apply requested",
                TraceIdUtil.getTraceId(), request.benefitOrderNo());
        return Result.success(refundService.apply(request.benefitOrderNo(), request.reason()));
    }

    @GetMapping("/result/{refundId}")
    public Result<RefundResultResponse> getResult(@PathVariable String refundId) {
        log.info("traceId={} bizOrderNo={} refund result requested",
                TraceIdUtil.getTraceId(), refundId);
        return Result.success(refundService.getResult(refundId));
    }
}
