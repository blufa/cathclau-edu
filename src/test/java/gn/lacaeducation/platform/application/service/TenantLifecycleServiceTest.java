package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.platform.application.event.TenantActivatedEvent;
import gn.lacaeducation.platform.application.event.TenantArchivedEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantLifecycleServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks TenantLifecycleService service;

    private Tenant tenant(TenantStatus status) {
        Tenant t = Tenant.builder().id(1L).schoolName("École Test").email("dir@ecole.gn")
                .director("Mamadou Diallo").status(status).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));
        return t;
    }

    private void stubSave() {
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void pendingVerification_toTrial_activatesAndPublishes() {
        tenant(TenantStatus.PENDING_VERIFICATION);
        stubSave();

        Tenant result = service.transition(1L, TenantStatus.TRIAL, "email verified");

        assertThat(result.getStatus()).isEqualTo(TenantStatus.TRIAL);
        assertThat(result.getActivatedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(TenantActivatedEvent.class));
    }

    @Test
    void active_toSuspended_publishesSuspended() {
        tenant(TenantStatus.ACTIVE);
        stubSave();
        service.transition(1L, TenantStatus.SUSPENDED, "manual");
        verify(eventPublisher).publishEvent(any(TenantSuspendedEvent.class));
    }

    @Test
    void suspended_toActive_publishesReactivated() {
        tenant(TenantStatus.SUSPENDED);
        stubSave();
        service.transition(1L, TenantStatus.ACTIVE, "regularised");
        verify(eventPublisher).publishEvent(any(TenantReactivatedEvent.class));
    }

    @Test
    void active_toArchived_publishesArchived() {
        tenant(TenantStatus.ACTIVE);
        stubSave();
        service.transition(1L, TenantStatus.ARCHIVED, "soft delete");
        verify(eventPublisher).publishEvent(any(TenantArchivedEvent.class));
    }

    @Test
    void illegalTransition_isRejected_422() {
        tenant(TenantStatus.ARCHIVED); // terminal

        assertThatThrownBy(() -> service.transition(1L, TenantStatus.ACTIVE, "x"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Illegal tenant transition");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void pendingToSuspended_isIllegal() {
        tenant(TenantStatus.PENDING_VERIFICATION);
        assertThatThrownBy(() -> service.transition(1L, TenantStatus.SUSPENDED, "x"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sameStatus_isIdempotent_noEventNoSave() {
        Tenant t = Tenant.builder().id(1L).status(TenantStatus.TRIAL).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(t));

        Tenant result = service.transition(1L, TenantStatus.TRIAL, "replay");

        assertThat(result.getStatus()).isEqualTo(TenantStatus.TRIAL);
        verify(tenantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
