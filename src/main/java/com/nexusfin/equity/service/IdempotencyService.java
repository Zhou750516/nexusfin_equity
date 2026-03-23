package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.IdempotencyRecord;

public interface IdempotencyService {

    boolean isProcessed(String requestId);

    void markProcessed(String requestId, String bizType, String bizKey, String responseBody);

    IdempotencyRecord getByRequestId(String requestId);
}
