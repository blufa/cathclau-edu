package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import gn.lacaeducation.platform.TenantListPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * §retour censeur : "si on relance c'est en ce moment les sms vont, si on ne relance pas les sms ne
 * doivent pas partir" — ce job quotidien ne fait plus QUE marquer le statut OVERDUE des échéances en
 * retard (utile pour le suivi/filtrage), il ne déclenche plus aucun SMS automatique (l'ancien
 * EcheanceDepasseeEvent/onEcheanceDepassee republiait le même rappel chaque jour tant que la facture
 * restait impayée, sans qu'aucun admin ne l'ait demandé). La relance reste possible, mais uniquement
 * via l'audience manuelle "Payeurs en retard" (/communications, BroadcastAudience.OVERDUE_PAYERS).
 */
@ExtendWith(MockitoExtension.class)
class DeadlineCheckSchedulerTest {

    @Mock InstallmentRepository installmentRepository;
    @Mock TenantListPort tenantListPort;

    OverdueInstallmentProcessor processor;
    DeadlineCheckScheduler scheduler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        processor = new OverdueInstallmentProcessor(installmentRepository);
        scheduler = new DeadlineCheckScheduler(tenantListPort, processor);
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
    }

    @Test
    void marksInstallmentOverdue_withoutSendingAnySms() {
        Installment pending = Installment.builder().id(5L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(2))
                .status(InstallmentStatus.PENDING).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(pending));

        scheduler.checkOverdueDeadlines();

        ArgumentCaptor<Installment> saved = ArgumentCaptor.forClass(Installment.class);
        verify(installmentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
    }

    @Test
    void alreadyOverdue_neverResavedDayAfterDay() {
        Installment overdue = Installment.builder().id(6L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(10))
                .status(InstallmentStatus.OVERDUE).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(overdue));

        scheduler.checkOverdueDeadlines();

        verify(installmentRepository, never()).save(any());
    }
}
