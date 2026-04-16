package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.PaymentProtocolCallbackRequest;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.PaymentProtocolCallbackService;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/callbacks")
public class PaymentProtocolCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PaymentProtocolCallbackController.class);

    private final PaymentProtocolCallbackService paymentProtocolCallbackService;

    public PaymentProtocolCallbackController(PaymentProtocolCallbackService paymentProtocolCallbackService) {
        this.paymentProtocolCallbackService = paymentProtocolCallbackService;
    }

    @PostMapping("/payment-protocol")
    public Result<Void> handlePaymentProtocol(@Valid @RequestBody PaymentProtocolCallbackRequest request) {
        paymentProtocolCallbackService.handleCallback(new PaymentProtocolCallbackService.PaymentProtocolCallbackCommand(
                request.requestId(),
                request.memberId(),
                request.externalUserId(),
                request.providerCode(),
                request.protocolNo(),
                request.protocolStatus(),
                request.signRequestNo(),
                request.channelCode(),
                request.signedTs()
        ));
        log.info("traceId={} bizOrderNo={} payment protocol callback accepted",
                TraceIdUtil.getTraceId(),
                request.requestId());
        return Result.success(null);
    }
}
