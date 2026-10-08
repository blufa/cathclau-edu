package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.ClassTransferCreditSurplusEvent;
import gn.lacaeducation.billing.application.port.in.ClassTransferRefundUseCase;
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

    // Même préfixe que InvoiceService#applyEarlyPaymentDiscount / SchoolBillingStatsProviderAdapter
    // (pas de constante partagée dans ce codebase pour ce motif précis — dupliquée volontairement,
    // comme aux deux autres endroits). Sert à détecter si l'ancienne classe avait cette remise, pour
    // la reporter sur la nouvelle plutôt que de la perdre silencieusement.
    private static final String EARLY_PAYMENT_DISCOUNT_REASON_PREFIX = "Remise paiement anticipé : ";

    private final SchoolFeeRepository schoolFeeRepository;
    private final InvoiceUseCase invoiceUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final ClassTransferRefundUseCase classTransferRefundUseCase;

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

        // Retour comptable réel : un parent qui avait réglé toute l'année avec la remise "paiement
        // anticipé" doit en bénéficier aussi sur la nouvelle classe — sinon le crédit reporté (déjà
        // net de cette remise) se compare à un total de la nouvelle classe resté au plein tarif, ce
        // qui masque un vrai surplus à rembourser. Rejoué AVANT le report de crédit ci-dessous (tant
        // que les nouvelles factures sont encore impayées — condition exigée par
        // applyEarlyPaymentDiscount) pour que le crédit se compare ensuite au total déjà remisé.
        boolean hadEarlyPaymentDiscount = oldInvoices.stream().anyMatch(i ->
                i.getDiscountReason() != null && i.getDiscountReason().startsWith(EARLY_PAYMENT_DISCOUNT_REASON_PREFIX));
        if (hadEarlyPaymentDiscount) {
            newInvoices = invoiceUseCase.applyEarlyPaymentDiscount(event.studentId(), newFee.getId());
        }

        BigDecimal remaining = credit;
        List<Invoice> sortedNew = newInvoices.stream()
                .sorted(Comparator.comparing(Invoice::getInstallmentId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (Invoice inv : sortedNew) {
            if (remaining.signum() <= 0) break;
            // Le solde encore net de remise (jamais le total BRUT) — sans ça, une remise (ci-dessus
            // ou une exonération déjà appliquée à la génération) serait reportée en trop, rendant le
            // solde restant négatif au lieu de rester à 0.
            BigDecimal toApply = remaining.min(inv.getRemainingBalance());
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
     * comptable, cas réel signalé). Persisté (visible sur le prochain reçu de l'élève tant qu'il
     * n'est pas marqué remboursé) ET publié pour l'alerte SMS immédiate à l'école. */
    private void publishSurplus(Long tenantId, EnrollmentClassChangedEvent event, BigDecimal surplus) {
        classTransferRefundUseCase.record(tenantId, event.studentId(), event.oldClassId(), event.newClassId(),
                surplus, "Transfert de classe (nouvelle classe moins chère)");
        eventPublisher.publishEvent(new ClassTransferCreditSurplusEvent(
                tenantId, event.studentId(), event.oldClassId(), event.newClassId(), surplus));
    }
}
