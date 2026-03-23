package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.entity.PaymentRecord;
import java.util.List;

public interface ReconciliationService {

    BenefitOrder queryOrderByBenefitOrderNo(String benefitOrderNo);

    List<BenefitOrder> queryOrdersByMemberId(String memberId);

    PaymentRecord queryPaymentByPaymentNo(String paymentNo);

    List<NotificationReceiveLog> queryByBenefitOrderNo(String benefitOrderNo);

    List<NotificationReceiveLog> queryByRequestId(String requestId);
}
