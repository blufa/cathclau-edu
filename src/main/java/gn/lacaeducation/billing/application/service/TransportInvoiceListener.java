package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.transport.application.event.TransportSubscriptionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Listens to TransportSubscriptionCreatedEvent and invoices the student against the TRANSPORT school
 * fee already configured (with its own échéances — mensuelles, trimestrielles ou annuelle, exactly
 * like tuition) for the subscription's ROUTE — not the student's class, since a bus line carries
 * students from different classes and the price depends on the route (distance), not the grade —
 * reusing the same tranche-based generation as MONTHLY fees ({@link InvoiceUseCase#generateMonthly}).
 * If no TRANSPORT fee (or no échéances) is configured yet for the route, this only logs a warning: the
 * subscription itself still succeeds, and invoices catch up automatically next time the fee is complete.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransportInvoiceListener {

    private final SchoolFeeRepository schoolFeeRepository;
    private final InvoiceUseCase invoiceUseCase;

    @EventListener
    @Transactional
    public void onTransportSubscriptionCreated(TransportSubscriptionCreatedEvent event) {
        List<SchoolFee> fees = schoolFeeRepository.findByRouteIdAndType(event.routeId(), FeeType.TRANSPORT);
        if (fees.isEmpty()) {
            log.warn("Aucun frais TRANSPORT configuré pour la ligne id={} (tenant={}) ; AUCUNE facture transport créée pour "
                            + "l'élève id={}. Configurez le frais (mensuel, trimestriel ou annuel, avec ses échéances) dans "
                            + "Frais scolaires, en le rattachant à cette ligne.",
                    event.routeId(), event.tenantId(), event.studentId());
            return;
        }

        for (SchoolFee fee : fees) {
            try {
                List<Invoice> created = invoiceUseCase.generateMonthly(
                        new GenerateMonthlyCommand(event.tenantId(), event.studentId(), fee.getId(), null)).created();
                log.info("{} facture(s) transport générée(s) pour l'élève id={} frais id={} (ligne id={})",
                        created.size(), event.studentId(), fee.getId(), event.routeId());
            } catch (BusinessRuleException ex) {
                // Ex. aucune échéance configurée pour ce frais transport, ou élève pas encore inscrit :
                // n'empêche pas l'abonnement, la facturation rattrapera dès que le frais sera complet.
                log.warn("Facturation transport différée pour l'élève id={} frais id={} : {}",
                        event.studentId(), fee.getId(), ex.getMessage());
            }
        }
    }
}
