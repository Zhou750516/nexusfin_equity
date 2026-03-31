package com.nexusfin.equity.thirdparty.techplatform;

public interface TechPlatformClient {

    TechPlatformNotifyResponse notifyCreditStatus(CreditStatusNoticeRequest request);

    TechPlatformNotifyResponse notifyLoanInfo(LoanInfoNoticeRequest request);

    TechPlatformNotifyResponse notifyRepaymentInfo(RepayInfoNoticeRequest request);
}
