package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.ScheduledBroadcastUseCase;
import gn.lacaeducation.platform.TenantListPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledBroadcastDispatchSchedulerTest {

    @Mock TenantListPort tenantListPort;
    @Mock ScheduledBroadcastUseCase scheduledBroadcastUseCase;
    @InjectMocks ScheduledBroadcastDispatchScheduler scheduler;

    @Test
    void dispatchesForEveryActiveTenant() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(
                new TenantListPort.TenantRef(1L), new TenantListPort.TenantRef(2L)));

        scheduler.dispatchDueBroadcasts();

        verify(scheduledBroadcastUseCase).processDueForTenant(eq(1L), any(LocalDateTime.class));
        verify(scheduledBroadcastUseCase).processDueForTenant(eq(2L), any(LocalDateTime.class));
    }

    @Test
    void oneTenantFailure_doesNotBlockOthers() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(
                new TenantListPort.TenantRef(1L), new TenantListPort.TenantRef(2L)));
        doThrow(new RuntimeException("boom")).when(scheduledBroadcastUseCase).processDueForTenant(eq(1L), any());

        scheduler.dispatchDueBroadcasts();

        verify(scheduledBroadcastUseCase, times(1)).processDueForTenant(eq(2L), any(LocalDateTime.class));
    }
}
