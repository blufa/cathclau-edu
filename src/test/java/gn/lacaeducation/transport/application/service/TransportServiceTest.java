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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * §retour comptable : "savoir qui a payé le transport" — seul {@link TransportService#paidStudentIds}
 * est testé ici (aucune suite de test n'existait pour ce module avant ce correctif).
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
    void paidStudentIds_delegatesToInvoiceQueryPort() {
        when(invoiceQueryPort.findStudentIdsWithPaidTransportInvoice(10L)).thenReturn(List.of(7L, 8L));

        List<Long> result = service.paidStudentIds(10L);

        assertThat(result).containsExactly(7L, 8L);
    }
}
