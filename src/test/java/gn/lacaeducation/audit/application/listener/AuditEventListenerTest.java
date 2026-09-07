package gn.lacaeducation.audit.application.listener;

import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase.RecordAuditCommand;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.application.event.RoleAssignedEvent;
import gn.lacaeducation.iam.application.event.UserLoggedInEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock AuditLogUseCase auditLogUseCase;

    AuditEventListener listener() {
        return new AuditEventListener(auditLogUseCase);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void onUserLoggedIn_recordsTenantScopedLogin() {
        listener().onUserLoggedIn(new UserLoggedInEvent(7L, 10L, LocalDateTime.now()));

        ArgumentCaptor<RecordAuditCommand> captor = ArgumentCaptor.forClass(RecordAuditCommand.class);
        verify(auditLogUseCase).record(captor.capture());
        RecordAuditCommand c = captor.getValue();
        assertThat(c.tenantId()).isEqualTo(10L);
        assertThat(c.utilisateurId()).isEqualTo(7L);
        assertThat(c.action()).isEqualTo("LOGIN");
        assertThat(c.ressource()).isEqualTo("User");
        assertThat(c.ressourceId()).isEqualTo(7L);
    }

    @Test
    void onRoleAssigned_resolvesTenantFromRequestContext() {
        TenantContext.set(42L);

        listener().onRoleAssigned(new RoleAssignedEvent(7L, 3L));

        ArgumentCaptor<RecordAuditCommand> captor = ArgumentCaptor.forClass(RecordAuditCommand.class);
        verify(auditLogUseCase).record(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(42L);
        assertThat(captor.getValue().details()).contains("roleId=3");
    }

    @Test
    void onTenantCreated_neverLeaksAdminPassword() {
        listener().onTenantCreated(new TenantCreatedEvent(5L, "École Test", "admin@ecole.gn",
                "Directeur X", "s3cr3t-p@ssword", true, "admin", null));

        ArgumentCaptor<RecordAuditCommand> captor = ArgumentCaptor.forClass(RecordAuditCommand.class);
        verify(auditLogUseCase).record(captor.capture());
        RecordAuditCommand c = captor.getValue();
        assertThat(c.tenantId()).isNull(); // action plateforme, pas rattachée au journal de l'école
        assertThat(c.ressourceId()).isEqualTo(5L);
        assertThat(c.details()).doesNotContain("s3cr3t-p@ssword").contains("École Test");
    }

    @Test
    void onTenantSuspended_isPlatformScoped_notTenantScoped() {
        listener().onTenantSuspended(new TenantSuspendedEvent(5L));

        ArgumentCaptor<RecordAuditCommand> captor = ArgumentCaptor.forClass(RecordAuditCommand.class);
        verify(auditLogUseCase).record(captor.capture());
        assertThat(captor.getValue().tenantId()).isNull();
        assertThat(captor.getValue().ressourceId()).isEqualTo(5L);
    }

    @Test
    void onPaymentSucceeded_isTenantScoped() {
        listener().onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, 55L, new BigDecimal("25000")));

        ArgumentCaptor<RecordAuditCommand> captor = ArgumentCaptor.forClass(RecordAuditCommand.class);
        verify(auditLogUseCase).record(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(10L);
        assertThat(captor.getValue().ressource()).isEqualTo("Payment");
        assertThat(captor.getValue().details()).contains("invoiceId=55").contains("25000");
    }

    @Test
    void loggingFailure_neverPropagatesToCaller() {
        org.mockito.Mockito.doThrow(new RuntimeException("db down")).when(auditLogUseCase).record(org.mockito.ArgumentMatchers.any());

        // Ne doit lancer aucune exception malgré l'échec d'écriture.
        listener().onUserLoggedIn(new UserLoggedInEvent(7L, 10L, LocalDateTime.now()));
    }
}
