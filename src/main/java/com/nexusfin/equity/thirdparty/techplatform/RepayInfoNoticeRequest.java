package com.nexusfin.equity.thirdparty.techplatform;

import java.math.BigDecimal;
import java.util.List;

public record RepayInfoNoticeRequest(
        List<RepayItem> repayList
) {

    public record RepayItem(
            String orderId,
            String repayNo,
            int period,
            String repayType,
            long repayTime,
            String repayStatus,
            String remark,
            int overDueDays,
            BigDecimal shouldRepayAmount,
            BigDecimal repayedAmount,
            BigDecimal repayOverDueFee,
            BigDecimal dueOverDueFee,
            BigDecimal repayPrincipal,
            BigDecimal duePrincipal,
            BigDecimal repayInterest,
            BigDecimal dueRepayInterest,
            BigDecimal repayTotalFee,
            BigDecimal dueRepayTotalFee,
            List<FeePlanItem> feePlan,
            String repayBankCard,
            String repayBank
    ) {
    }

    public record FeePlanItem(
            String feeCode,
            String feeType,
            BigDecimal dueRepayAmt,
            BigDecimal repayAmt
    ) {
    }
}
