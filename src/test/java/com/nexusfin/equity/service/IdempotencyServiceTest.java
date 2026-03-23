package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.service.impl.IdempotencyServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @Test
    void shouldInsertRecordWhenRequestNotProcessed() {
        when(idempotencyRecordRepository.selectById("req-1")).thenReturn(null);

        idempotencyService.markProcessed("req-1", "REGISTER", "mem-1", "body");

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).insert(captor.capture());
        assertThat(captor.getValue().getRequestId()).isEqualTo("req-1");
        assertThat(captor.getValue().getBizType()).isEqualTo("REGISTER");
        assertThat(captor.getValue().getBizKey()).isEqualTo("mem-1");
        assertThat(captor.getValue().getProcessedTs()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldSkipInsertWhenRequestAlreadyProcessed() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestId("req-1");
        when(idempotencyRecordRepository.selectById("req-1")).thenReturn(record);

        idempotencyService.markProcessed("req-1", "REGISTER", "mem-1", "body");

        verify(idempotencyRecordRepository, never()).insert(any());
    }

    @Test
    void shouldReturnStoredRecordByRequestId() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestId("req-2");
        when(idempotencyRecordRepository.selectById("req-2")).thenReturn(record);

        IdempotencyRecord result = idempotencyService.getByRequestId("req-2");

        assertThat(result).isSameAs(record);
    }
}
