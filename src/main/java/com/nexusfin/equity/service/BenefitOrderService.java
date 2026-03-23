package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitOrderStatusResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.ExerciseUrlResponse;
import com.nexusfin.equity.dto.response.ProductPageResponse;

public interface BenefitOrderService {

    ProductPageResponse getProductPage(String productCode, String memberId);

    CreateBenefitOrderResponse createOrder(CreateBenefitOrderRequest request);

    BenefitOrderStatusResponse getOrderStatus(String benefitOrderNo);

    ExerciseUrlResponse getExerciseUrl(String benefitOrderNo);
}
