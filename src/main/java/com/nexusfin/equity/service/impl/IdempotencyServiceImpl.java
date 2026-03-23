package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.service.IdempotencyService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyServiceImpl(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Override
    public boolean isProcessed(String requestId) {
        return idempotencyRecordRepository.selectById(requestId) != null;
    }

    @Override
    public void markProcessed(String requestId, String bizType, String bizKey, String responseBody) {
        if (isProcessed(requestId)) {
            return;
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestId(requestId);
        record.setBizType(bizType);
        record.setBizKey(bizKey);
        record.setResponseBody(responseBody);
        record.setProcessedTs(LocalDateTime.now());
        idempotencyRecordRepository.insert(record);
    }

    @Override
    public IdempotencyRecord getByRequestId(String requestId) {
        return idempotencyRecordRepository.selectById(requestId);
    }
}
