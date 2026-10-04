package gn.lacaeducation.audit.application.service;

import gn.lacaeducation.audit.application.port.in.AccountantAnomalyAlertUseCase;
import gn.lacaeducation.platform.TenantListPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable). */
@ExtendWith(MockitoExtension.class)
class AccountantAnomalyAlertSchedulerTest {

    @Mock TenantListPort tenantListPort;
    @Mock AccountantAnomalyAlertUseCase useCase;
    @InjectMocks AccountantAnomalyAlertScheduler scheduler;

    @AfterEach
    void clearContext() {
        gn.lacaeducation.common.TenantContext.clear();
    }

    @Test
    void evaluateAllTenants_oneTenantFails_othersStillEvaluated() {
        when(tenantListPort.listActiveTenantIds())
                .thenReturn(List.of(new TenantListPort.TenantRef(10L), new TenantListPort.TenantRef(20L)));
        doThrow(new RuntimeException("boom")).when(useCase).evaluateForTenant(10L);

        scheduler.evaluateAllTenants();

        verify(useCase).evaluateForTenant(10L);
        verify(useCase).evaluateForTenant(20L);
    }
}
