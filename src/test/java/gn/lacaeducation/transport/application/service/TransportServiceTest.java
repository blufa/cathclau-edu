package gn.lacaeducation.transport.application.service;

import gn.lacaeducation.transport.application.port.out.TransportInvoiceQueryPort;
import gn.lacaeducation.transport.application.port.out.TransportRouteRepository;
import gn.lacaeducation.transport.application.port.out.TransportStopRepository;
import gn.lacaeducation.transport.application.port.out.TransportSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.YearMonth;
import java.util.Map;
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
}
