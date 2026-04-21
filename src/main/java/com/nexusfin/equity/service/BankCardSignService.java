package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BankCardSignApplyRequest;
import com.nexusfin.equity.dto.request.BankCardSignConfirmRequest;
import com.nexusfin.equity.dto.response.BankCardSignApplyResponse;
import com.nexusfin.equity.dto.response.BankCardSignConfirmResponse;
import com.nexusfin.equity.dto.response.BankCardSignStatusResponse;

public interface BankCardSignService {

    BankCardSignStatusResponse getSignStatus(String memberId, String accountNo);

    BankCardSignApplyResponse applySign(String memberId, BankCardSignApplyRequest request);

    BankCardSignConfirmResponse confirmSign(String memberId, BankCardSignConfirmRequest request);
}
