package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.students.application.event.EnrollmentAbandonedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quand un élève abandonne en cours d'année (§students, {@link EnrollmentAbandonedEvent}), toutes ses
 * factures encore dues (UNPAID/PARTIAL) sont annulées ({@link InvoiceUseCase#voidInvoice}, historique
 * conservé) — la famille n'a plus rien à régler pour des mois où l'élève ne viendra plus. Ce qui a
 * déjà été encaissé ne bouge jamais : {@code voidInvoice} ne touche pas {@code paidAmount}, seule la
 * part encore due ({@code remainingBalance}) est remise à zéro ; une facture déjà PAYÉE n'est jamais
 * annulée par ce listener (filtrée avant l'appel). Scopé à l'année scolaire de l'inscription abandonnée
 * pour ne jamais toucher les factures d'années antérieures ou d'une éventuelle réinscription future.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentAbandonedInvoiceListener {

    private final InvoiceUseCase invoiceUseCase;

    @EventListener
    @Transactional
    public void onEnrollmentAbandoned(EnrollmentAbandonedEvent event) {
        List<Invoice> due = invoiceUseCase.findByStudentId(event.studentId()).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .filter(i -> event.academicYearId() == null || event.academicYearId().equals(i.getAcademicYearId()))
                .toList();

        if (due.isEmpty()) {
            log.info("Abandon : aucune facture due à annuler pour l'élève id={}", event.studentId());
            return;
        }

        due.forEach(i -> invoiceUseCase.voidInvoice(i.getId()));
        log.info("Abandon : {} facture(s) due(s) annulée(s) pour l'élève id={} (classe id={}) — montants déjà "
                + "encaissés conservés.", due.size(), event.studentId(), event.classId());
    }
}
