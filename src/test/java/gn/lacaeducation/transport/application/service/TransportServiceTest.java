package gn.lacaeducation.transport.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import gn.lacaeducation.transport.application.port.out.TransportInvoiceQueryPort;
import gn.lacaeducation.transport.application.port.out.TransportRouteRepository;
import gn.lacaeducation.transport.application.port.out.TransportStopRepository;
import gn.lacaeducation.transport.application.port.out.TransportSubscriptionRepository;
import gn.lacaeducation.transport.domain.SubscriptionStatus;
import gn.lacaeducation.transport.domain.TransportDirection;
import gn.lacaeducation.transport.domain.TransportRoute;
import gn.lacaeducation.transport.domain.TransportSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * §retour comptable : "savoir qui a payé le transport", précisé en "le nombre de mois payé" — seul
 * {@link TransportService#paidMonthsCountByStudentId} est testé ici (aucune suite de test n'existait
 * pour ce module avant ce correctif).
 */
@ExtendWith(MockitoExtension.class)
class TransportServiceTest {

    @Mock TransportRouteRepository routeRepo;
    @Mock TransportStopRepository stopRepo;
    @Mock TransportSubscriptionRepository subRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TransportInvoiceQueryPort invoiceQueryPort;
    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @InjectMocks TransportService service;

    @Test
    void paidMonthsCountByStudentId_delegatesToInvoiceQueryPort() {
        when(invoiceQueryPort.countPaidTransportMonthsByStudentId(10L)).thenReturn(Map.of(7L, 2L, 8L, 1L));

        Map<Long, Long> result = service.paidMonthsCountByStudentId(10L);

        assertThat(result).containsEntry(7L, 2L).containsEntry(8L, 1L);
    }

    @Test
    void studentIdsPaidForMonth_delegatesToInvoiceQueryPort_withYearMonthString() {
        // §retour utilisateur : "si tu filtres par mois rien ne vient" — le filtre doit porter sur le
        // mois réellement couvert ("2026-10"), jamais sur des bornes de date d'échéance (qui tombait
        // le mois suivant par convention et décalait tout le filtre).
        YearMonth october = YearMonth.of(2026, 10);
        when(invoiceQueryPort.findStudentIdsPaidForMonth(10L, "2026-10")).thenReturn(Set.of(7L, 8L));

        Set<Long> result = service.studentIdsPaidForMonth(10L, october);

        assertThat(result).containsExactlyInAnyOrder(7L, 8L);
    }

    @Test
    void printableSubscriptions_resolvesStudentClassAndRouteName() {
        // §retour comptable : "ajouter la classe des élèves, et qu'il puisse l'imprimer" sur la liste
        // des transports — matricule/classe résolus ici, le PDF ne fait que les afficher.
        TransportSubscription sub = TransportSubscription.builder().id(1L).routeId(5L).studentId(7L)
                .direction(TransportDirection.BOTH).status(SubscriptionStatus.ACTIVE).build();
        when(subRepo.findById(1L)).thenReturn(Optional.of(sub));
        when(routeRepo.findByTenantId(10L)).thenReturn(List.of(
                TransportRoute.builder().id(5L).name("Ligne Kaloum").build()));
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(
                new StudentPublicInfo(7L, 10L, "Fatoumata", "Bah", null, null, "GSBD24-007", null, null, null, null)));
        when(studentDataProvider.findCurrentClassId(7L)).thenReturn(Optional.of(20L));
        when(academicsDataProvider.findClass(20L)).thenReturn(Optional.of(
                new ClassPublicInfo(20L, "CM2 A", "CM2", "NORMALE", null, null, "PRIMAIRE")));
        when(invoiceQueryPort.countPaidTransportMonthsByStudentId(10L)).thenReturn(Map.of(7L, 3L));

        List<TransportService.SubscriptionPrintRow> rows = service.printableSubscriptions(10L, List.of(1L));

        assertThat(rows).hasSize(1);
        TransportService.SubscriptionPrintRow row = rows.get(0);
        assertThat(row.studentNumber()).isEqualTo("GSBD24-007");
        assertThat(row.studentName()).isEqualTo("Fatoumata Bah");
        assertThat(row.className()).isEqualTo("CM2 A");
        assertThat(row.routeName()).isEqualTo("Ligne Kaloum");
        assertThat(row.paidMonthsCount()).isEqualTo(3L);
    }

    @Test
    void printableSubscriptions_unknownSubscriptionId_silentlySkipped() {
        when(subRepo.findById(99L)).thenReturn(Optional.empty());
        when(routeRepo.findByTenantId(10L)).thenReturn(List.of());
        when(invoiceQueryPort.countPaidTransportMonthsByStudentId(10L)).thenReturn(Map.of());

        List<TransportService.SubscriptionPrintRow> rows = service.printableSubscriptions(10L, List.of(99L));

        assertThat(rows).isEmpty();
    }
}
