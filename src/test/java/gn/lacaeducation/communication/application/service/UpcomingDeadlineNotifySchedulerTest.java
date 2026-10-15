package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.platform.TenantListPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La logique métier (digest, résolution des destinataires) est couverte par
 * {@link UpcomingDeadlineNotifyProcessorTest} — ce test vérifie seulement la boucle par tenant et le
 * relais vers le processeur transactionnel (voir sa javadoc : nécessaire pour que
 * {@code @TransactionalEventListener} se déclenche, un appel {@code this.xxx()} depuis cette classe ne
 * suffirait pas). */
@ExtendWith(MockitoExtension.class)
class UpcomingDeadlineNotifySchedulerTest {

    @Mock TenantListPort tenantListPort;
    @Mock UpcomingDeadlineNotifyProcessor processor;

    UpcomingDeadlineNotifyScheduler scheduler() {
        return new UpcomingDeadlineNotifyScheduler(tenantListPort, processor);
    }

    @Test
    void notifyUpcomingDeadlines_delegatesToProcessorPerActiveTenant() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(
                List.of(new TenantListPort.TenantRef(1L), new TenantListPort.TenantRef(2L)));

        scheduler().notifyUpcomingDeadlines();

        verify(processor).notifyForTenant(1L);
        verify(processor).notifyForTenant(2L);
    }

    @Test
    void notifyForTenant_delegatesSingleTenantToProcessor() {
        scheduler().notifyForTenant(5L);

        verify(processor).notifyForTenant(5L);
    }
}
