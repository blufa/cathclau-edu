package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.ClassTransferCreditSurplusEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
 * coup régénérait les factures de mensualité de 7ème SANS annuler celles de 6ème — les deux jeux de
 * factures restaient actifs en même temps).
 *
 * <p>Seules les mensualités (MENSUALITE) sont traitées ici ({@link #reconcileMonthly}) : annulées puis
 * régénérées pour la nouvelle classe, avec report en crédit de ce qui avait déjà été payé — seul frais
 * dont le report fait sens (un même nombre d'échéances, juste un montant différent). Les AUTRES frais
 * de l'ancienne classe (inscription, examen, cantine, transport, autre…) ne sont PAS touchés : une
 * version antérieure les annulait automatiquement, mais sans jamais les régénérer pour la nouvelle
 * classe (laissé au comptable, via "Générer les factures") — en pratique une facture annexe encore
 * impayée disparaissait purement et simplement de "Factures à régler" au lieu d'y rester (§retour
 * comptable réel, transfert testé en conditions réelles). Le risque de duplication pour les frais
 * annexes (ex. "Révision" générée deux fois) est un problème distinct, pas spécifique au transfert de
 * classe — voir le ticket correspondant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassTransferInvoiceListener {

    private final SchoolFeeRepository schoolFeeRepository;
    private final InvoiceUseCase invoiceUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void onEnrollmentClassChanged(EnrollmentClassChangedEvent event) {
        reconcileMonthly(event);
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
            publishSurplus(oldInvoices.get(0).getTenantId(), event, credit);
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
            invoiceUseCase.applyCredit(inv.getId(), toApply, "Transfert de classe");
            remaining = remaining.subtract(toApply);
        }
        if (remaining.signum() > 0) {
            log.warn("Transfert de classe : reliquat de crédit {} non affecté (nouvelles mensualités insuffisantes) "
                    + "pour l'élève id={} — à régulariser manuellement.", remaining, event.studentId());
            publishSurplus(newFee.getTenantId(), event, remaining);
        }
    }

    /** Surplus jamais reporté sur aucune facture (le parent avait déjà réglé plus que ce que coûte la
     * nouvelle classe) — à rembourser, pas à laisser filer silencieusement dans les logs (§retour
     * comptable, cas réel signalé). Consommé par {@code communication} pour alerter l'école par SMS. */
    private void publishSurplus(Long tenantId, EnrollmentClassChangedEvent event, BigDecimal surplus) {
        eventPublisher.publishEvent(new ClassTransferCreditSurplusEvent(
                tenantId, event.studentId(), event.oldClassId(), event.newClassId(), surplus));
    }
}
