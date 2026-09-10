package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.GenerateMonthlyCommand;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.students.application.event.EnrollmentClassChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Réconcilie la facturation quand un élève est transféré vers une autre classe en cours d'année
 * (§billing, réponse à Correction-hier.md) : seules les mensualités (frais MENSUALITE) diffèrent
 * réellement d'une classe à l'autre — les frais d'inscription déjà payés ne sont jamais re-facturés,
 * l'élève ne se réinscrit pas. Les mensualités non annulées de l'ancienne classe sont annulées
 * ({@link InvoiceUseCase#voidInvoice}, historique conservé) et tout ce qui avait déjà été payé dessus
 * est reporté en crédit sur les nouvelles mensualités générées pour la nouvelle classe, dans l'ordre
 * chronologique des échéances — sans notifier le parent d'un « nouveau paiement » (ce n'en est pas
 * un). Si la nouvelle classe n'a aucun frais MENSUALITE configuré, ou si le crédit dépasse le montant
 * des nouvelles factures générées, le reliquat est simplement journalisé (pas de solde créditeur géré
 * sur une facture).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassTransferInvoiceListener {

    private final SchoolFeeRepository schoolFeeRepository;
    private final InvoiceUseCase invoiceUseCase;

    @EventListener
    @Transactional
    public void onEnrollmentClassChanged(EnrollmentClassChangedEvent event) {
        List<Long> oldMonthlyFeeIds = schoolFeeRepository.findByClassIdAndType(event.oldClassId(), FeeType.MONTHLY)
                .stream().map(SchoolFee::getId).toList();

        List<Invoice> oldInvoices = invoiceUseCase.findByStudentId(event.studentId()).stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .filter(i -> event.academicYearId() == null || event.academicYearId().equals(i.getAcademicYearId()))
                .filter(i -> i.getFeeId() != null && oldMonthlyFeeIds.contains(i.getFeeId()))
                .toList();

        if (oldInvoices.isEmpty()) {
            log.info("Transfert de classe : aucune facture de mensualité à reporter pour l'élève id={} "
                    + "(ancienne classe id={})", event.studentId(), event.oldClassId());
            return;
        }

        BigDecimal credit = oldInvoices.stream()
                .map(Invoice::getPaidAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        oldInvoices.forEach(i -> invoiceUseCase.voidInvoice(i.getId()));
        log.info("Transfert de classe : {} facture(s) de mensualité annulée(s) pour l'élève id={}, crédit à reporter={}",
                oldInvoices.size(), event.studentId(), credit);

        List<SchoolFee> newFees = schoolFeeRepository.findByClassIdAndType(event.newClassId(), FeeType.MONTHLY);
        if (newFees.isEmpty()) {
            log.warn("Transfert de classe : aucun frais MENSUALITE configuré pour la nouvelle classe id={} ; "
                            + "crédit de {} non reporté pour l'élève id={} — à régulariser manuellement.",
                    event.newClassId(), credit, event.studentId());
            return;
        }
        SchoolFee newFee = newFees.get(0);

        List<Invoice> newInvoices;
        try {
            newInvoices = invoiceUseCase.generateMonthly(
                    new GenerateMonthlyCommand(newFee.getTenantId(), event.studentId(), newFee.getId(), event.academicYearId()));
        } catch (RuntimeException ex) {
            log.error("Transfert de classe : échec de génération des mensualités de la nouvelle classe id={} "
                    + "pour l'élève id={} : {}", event.newClassId(), event.studentId(), ex.getMessage(), ex);
            return;
        }

        BigDecimal remaining = credit;
        List<Invoice> sortedNew = newInvoices.stream()
                .sorted(Comparator.comparing(Invoice::getInstallmentId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (Invoice inv : sortedNew) {
            if (remaining.signum() <= 0) break;
            BigDecimal toApply = remaining.min(inv.getTotalAmount());
            invoiceUseCase.applyCredit(inv.getId(), toApply);
            remaining = remaining.subtract(toApply);
        }
        if (remaining.signum() > 0) {
            log.warn("Transfert de classe : reliquat de crédit {} non affecté (nouvelles mensualités insuffisantes) "
                    + "pour l'élève id={} — à régulariser manuellement.", remaining, event.studentId());
        }
    }
}
