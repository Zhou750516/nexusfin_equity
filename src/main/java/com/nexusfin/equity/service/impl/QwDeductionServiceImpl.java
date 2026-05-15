package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.service.QwDeductionService;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwDeductionQueryRequest;
import com.nexusfin.equity.thirdparty.qw.QwDeductionQueryResponse;
import com.nexusfin.equity.thirdparty.qw.QwOrderCancelRequest;
import com.nexusfin.equity.thirdparty.qw.QwOrderCancelResponse;
import org.springframework.stereotype.Service;

@Service
public class QwDeductionServiceImpl implements QwDeductionService {

    private final QwBenefitClient qwBenefitClient;

    public QwDeductionServiceImpl(QwBenefitClient qwBenefitClient) {
        this.qwBenefitClient = qwBenefitClient;
    }

    @Override
    public QwDeductionQueryResponse queryDeduction(String uniqueId, String partnerOrderNo) {
        return qwBenefitClient.queryDeduction(new QwDeductionQueryRequest(uniqueId, partnerOrderNo));
    }

    @Override
    public QwOrderCancelResponse cancelOrder(String partnerOrderNo) {
        return qwBenefitClient.cancelOrder(new QwOrderCancelRequest(partnerOrderNo));
    }
}
