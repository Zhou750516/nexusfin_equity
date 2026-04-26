package com.nexusfin.equity.service;

import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncRequest;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmRequest;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendResponse;
import com.nexusfin.equity.thirdparty.yunka.CreditImageQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.CreditImageQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanRequest;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanResponse;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.UserQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserTokenRequest;
import com.nexusfin.equity.thirdparty.yunka.UserTokenResponse;

public interface XiaohuaGatewayService {

    ProtocolQueryResponse queryProtocols(String requestId, String bizOrderNo, ProtocolQueryRequest request);

    UserTokenResponse validateUserToken(String requestId, String bizOrderNo, UserTokenRequest request);

    UserQueryResponse queryUser(String requestId, String bizOrderNo, UserQueryRequest request);

    UserCardListResponse queryUserCards(String requestId, String bizOrderNo, UserCardListRequest request);

    LoanRepayPlanResponse queryLoanRepayPlan(String requestId, String bizOrderNo, LoanRepayPlanRequest request);

    CardSmsSendResponse sendCardSms(String requestId, String bizOrderNo, CardSmsSendRequest request);

    CardSmsConfirmResponse confirmCardSms(String requestId, String bizOrderNo, CardSmsConfirmRequest request);

    CreditImageQueryResponse queryCreditImages(String requestId, String bizOrderNo, CreditImageQueryRequest request);

    BenefitOrderSyncResponse syncBenefitOrder(String requestId, String bizOrderNo, BenefitOrderSyncRequest request);
}
