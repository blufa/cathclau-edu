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
 * Réconcilie la facturation quand un élève change de classe — que ce soit un transfert en cours
 * d'année sur une inscription VALIDÉE, ou la correction d'une classe mal saisie juste après une
 * inscription encore EN ATTENTE (§billing, retour comptable réel : corriger 6ème en 7ème juste après
 * coup régénérait les factures de 7ème SANS annuler celles de 6ème — les deux jeux de factures
 * restaient actifs en même temps, y compris pour les frais annexes comme "Révision").
 *
 * <p>Les mensualités (MENSUALITE) ont un traitement dédié ({@link #reconcileMonthly}) : annulées puis
 * régénérées pour la nouvelle classe, avec report en crédit de ce qui avait déjà été payé — seul frais
 * dont le report fait sens (un même nombre d'échéances, juste un montant différent). Tous les AUTRES
 * frais de l'ancienne classe (inscription, examen, cantine, transport, autre…) sont simplement
 * ANNULÉS ({@link #voidOtherOldClassInvoices}) — leur régénération pour la nouvelle classe, si
 * nécessaire, passe par les mécanismes déjà en place (frais d'inscription : automatique via
 * {@code EnrollmentInvoiceListener} ; autres frais annexes : action "Générer les factures" du
 * comptable) — ne pas les dupliquer ici. Si une facture annulée avait déjà un paiement dessus, le
 * montant payé n'est PAS reporté automatiquement (contrairement aux mensualités : la correspondance
 * frais-par-frais entre les deux classes est trop ambiguë pour l'automatiser sans risque) — journalisé
 * pour régularisation manuelle, jamais perdu silencieusement.
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
        reconcileMonthly(event);
        voidOtherOldClassInvoices(event);
    }

    private void voidOtherOldClassInvoices(EnrollmentClassChangedEvent event) {
        List<Long> oldOtherFeeIds = schoolFeeRepository.findByClassId(event.oldClassId()).stream()
                .filter(f -> f.getType() != FeeType.MONTHLY)
                .map(SchoolFee::getId)
                .toList();
        if (oldOtherFeeIds.isEmpty()) return;

        List<Invoice> toVoid = invoiceUseCase.findByStudentId(event.studentId()).stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .filter(i -> event.academicYearId() == null || event.academicYearId().equals(i.getAcademicYearId()))
                .filter(i -> i.getFeeId() != null && oldOtherFeeIds.contains(i.getFeeId()))
                .toList();
        if (toVoid.isEmpty()) return;

        for (Invoice inv : toVoid) {
            BigDecimal paid = inv.getPaidAmount();
            if (paid != null && paid.signum() > 0) {
                log.warn("Transfert de classe : facture {} (frais id={}) de l'ancienne classe id={} annulée alors qu'elle "
                                + "portait déjà {} payé pour l'élève id={} — montant à régulariser manuellement, pas reporté automatiquement.",
                        inv.getReference(), inv.getFeeId(), event.oldClassId(), paid, event.studentId());
            }
            invoiceUseCase.voidInvoice(inv.getId());
        }
        log.info("Transfert de classe : {} facture(s) annexe(s) (hors mensualité) de l'ancienne classe id={} annulée(s) pour l'élève id={}",
                toVoid.size(), event.oldClassId(), event.studentId());
    }

    private void reconcileMonthly(EnrollmentClassChangedEvent event) {
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
                    new GenerateMonthlyCommand(newFee.getTenantId(), event.studentId(), newFee.getId(), event.academicYearId())).created();
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
