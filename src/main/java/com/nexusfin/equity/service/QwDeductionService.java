package com.nexusfin.equity.service;

import com.nexusfin.equity.thirdparty.qw.QwDeductionQueryResponse;
import com.nexusfin.equity.thirdparty.qw.QwOrderCancelResponse;

public interface QwDeductionService {

    QwDeductionQueryResponse queryDeduction(String uniqueId, String partnerOrderNo);

    QwOrderCancelResponse cancelOrder(String partnerOrderNo);
}
