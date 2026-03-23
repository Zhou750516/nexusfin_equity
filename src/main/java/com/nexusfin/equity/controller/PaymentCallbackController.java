package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.DeductionCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/callbacks")
public class PaymentCallbackController {

    private final PaymentService paymentService;

    public PaymentCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/first-deduction")
    public Result<PaymentStatusResponse> handleFirstDeduction(@Valid @RequestBody DeductionCallbackRequest request) {
        return Result.success(paymentService.handleFirstDeductCallback(request));
    }

    @PostMapping("/fallback-deduction")
    public Result<PaymentStatusResponse> handleFallbackDeduction(@Valid @RequestBody DeductionCallbackRequest request) {
        return Result.success(paymentService.handleFallbackDeductCallback(request));
    }
}
