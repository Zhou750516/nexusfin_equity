package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.BenefitOrder;

public interface DownstreamSyncService {

    void syncOrder(BenefitOrder order);
}
