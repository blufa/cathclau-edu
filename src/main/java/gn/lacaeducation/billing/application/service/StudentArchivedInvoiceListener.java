package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.students.application.event.StudentArchivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quand un élève est archivé (§students, {@link StudentArchivedEvent}), toutes ses factures encore
 * dues (UNPAID/PARTIAL, toutes années confondues — l'élève est parti, pas seulement une inscription
 * particulière) sont annulées ({@link InvoiceUseCase#voidInvoice}, historique conservé) — la famille
 * n'a plus rien à régler. Ce qui a déjà été encaissé ne bouge jamais : {@code voidInvoice} ne touche
 * pas {@code paidAmount}, seule la part encore due ({@code remainingBalance}) est remise à zéro ; une
 * facture déjà PAYÉE n'est jamais annulée par ce listener (filtrée avant l'appel). Même principe que
 * {@link EnrollmentAbandonedInvoiceListener}, sans la restriction à une année scolaire précise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentArchivedInvoiceListener {

    private final InvoiceUseCase invoiceUseCase;

    @EventListener
    @Transactional
    public void onStudentArchived(StudentArchivedEvent event) {
        List<Invoice> due = invoiceUseCase.findByStudentId(event.studentId()).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .toList();

        if (due.isEmpty()) {
            log.info("Archivage : aucune facture due à annuler pour l'élève id={}", event.studentId());
            return;
        }

        due.forEach(i -> invoiceUseCase.voidInvoice(i.getId()));
        log.info("Archivage : {} facture(s) due(s) annulée(s) pour l'élève id={} — montants déjà "
                + "encaissés conservés.", due.size(), event.studentId());
    }
}
