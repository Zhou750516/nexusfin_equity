package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitOrderStatusResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.ExerciseUrlResponse;
import com.nexusfin.equity.dto.response.ProductPageResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.BenefitOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/equity")
public class BenefitOrderController {

    private static final Logger log = LoggerFactory.getLogger(BenefitOrderController.class);

    private final BenefitOrderService benefitOrderService;

    public BenefitOrderController(BenefitOrderService benefitOrderService) {
        this.benefitOrderService = benefitOrderService;
    }

    @GetMapping("/products/{productCode}")
    public Result<ProductPageResponse> getProductPage(
            @PathVariable String productCode,
            @RequestParam(required = false) String memberId
    ) {
        return Result.success(benefitOrderService.getProductPage(productCode, memberId));
    }

    @PostMapping("/orders")
    public Result<CreateBenefitOrderResponse> createOrder(@Valid @RequestBody CreateBenefitOrderRequest request) {
        // Controller 只做参数接收和响应包装，订单编排全部放在 service 层。
        CreateBenefitOrderResponse response = benefitOrderService.createOrder(request);
        log.info("traceId={} bizOrderNo={} benefit order created via controller",
                com.nexusfin.equity.util.TraceIdUtil.getTraceId(), response.benefitOrderNo());
        return Result.success(response);
    }

    @GetMapping("/orders/{benefitOrderNo}")
    public Result<BenefitOrderStatusResponse> getOrderStatus(@PathVariable String benefitOrderNo) {
        return Result.success(benefitOrderService.getOrderStatus(benefitOrderNo));
    }

    @GetMapping("/exercise-url/{benefitOrderNo}")
    public Result<ExerciseUrlResponse> getExerciseUrl(@PathVariable String benefitOrderNo) {
        return Result.success(benefitOrderService.getExerciseUrl(benefitOrderNo));
    }
}
