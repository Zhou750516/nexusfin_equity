package com.nexusfin.equity.schedule;

import com.nexusfin.equity.service.AsyncCompensationSchedulerCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncCompensationSupervisorSchedulerTest {

    @Mock
    private AsyncCompensationSchedulerCoordinator coordinator;

    @Test
    void shouldSkipWhenSchedulingDisabled() {
        when(coordinator.isSchedulingEnabled()).thenReturn(false);
        AsyncCompensationSupervisorScheduler scheduler = new AsyncCompensationSupervisorScheduler(coordinator);

        scheduler.poll();

        verify(coordinator).isSchedulingEnabled();
        verify(coordinator, never()).isSupervisorEnabled();
        verify(coordinator, never()).runSupervisorTick();
    }

    @Test
    void shouldSkipWhenSupervisorDisabled() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isSupervisorEnabled()).thenReturn(false);
        AsyncCompensationSupervisorScheduler scheduler = new AsyncCompensationSupervisorScheduler(coordinator);

        scheduler.poll();

        verify(coordinator).isSchedulingEnabled();
        verify(coordinator).isSupervisorEnabled();
        verify(coordinator, never()).runSupervisorTick();
    }

    @Test
    void shouldRunSupervisorTickWhenEnabled() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isSupervisorEnabled()).thenReturn(true);
        AsyncCompensationSupervisorScheduler scheduler = new AsyncCompensationSupervisorScheduler(coordinator);

        scheduler.poll();

        verify(coordinator).runSupervisorTick();
    }

    @Test
    void shouldSwallowSchedulerExceptions() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isSupervisorEnabled()).thenReturn(true);
        doThrow(new IllegalStateException("supervisor failed"))
                .when(coordinator).runSupervisorTick();
        AsyncCompensationSupervisorScheduler scheduler = new AsyncCompensationSupervisorScheduler(coordinator);

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
        verify(coordinator).runSupervisorTick();
    }
}
