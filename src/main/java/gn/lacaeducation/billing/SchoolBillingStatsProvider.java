package gn.lacaeducation.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * API publique du module billing pour le reporting financier de l'école (§10.2) : agrégats de
 * facturation d'un tenant (facturé, encaissé, impayés, taux de recouvrement).
 */
public interface SchoolBillingStatsProvider {

    /**
     * @param invoiced      total facturé (somme des montants totaux)
     * @param collected     total encaissé (somme des montants payés)
     * @param outstanding   reste dû (somme des soldes restants)
     * @param recoveryRate  taux de recouvrement (encaissé / facturé, en %)
     * @param unpaidCount   nombre de factures impayées ou partielles
     */
    record BillingStats(BigDecimal invoiced, BigDecimal collected, BigDecimal outstanding,
                        BigDecimal recoveryRate, long unpaidCount) {}

    BillingStats billingStats(Long tenantId);

    // ── Vision Fondateur (features/14) : recouvrement détaillé par année scolaire ──

    record ClassRecovery(Long classId, BigDecimal invoiced, BigDecimal collected,
                         BigDecimal outstanding, BigDecimal recoveryRate) {}

    record PeriodRecovery(String periodLabel, BigDecimal invoiced, BigDecimal collected,
                          BigDecimal outstanding, BigDecimal recoveryRate) {}

    record TopDebtor(Long studentId, Long classId, BigDecimal outstanding) {}

    record RecoveryBreakdown(BillingStats overall, List<ClassRecovery> byClass,
                             List<PeriodRecovery> byPeriod, List<TopDebtor> topDebtors) {}

    /** Recouvrement d'une année scolaire donnée, ventilé par classe et par période/mensualité. */
    RecoveryBreakdown recoveryBreakdown(Long tenantId, Long academicYearId, int topDebtorLimit);

    /**
     * @param activeExemptionsCount  nombre d'exonérations actives (toutes catégories)
     * @param totalExemptions        exonérations totales (100 %) actives
     * @param partialExemptions      exonérations partielles actives
     * @param familyDiscountAmount   montant réellement déduit des factures via les remises fratrie
     *                               (seule remise effectivement appliquée aujourd'hui — §recherche)
     * @param studentsFullySettledMonthlyFees  élèves dont toutes les factures MENSUALITE de l'année sont PAYEE
     * @param estimatedEarlyPaymentSavings     estimation indicative (5 %) — AUCUNE remise réelle n'est
     *                                         appliquée pour un paiement anticipé aujourd'hui ; ce chiffre
     *                                         sert uniquement à évaluer l'intérêt d'introduire une telle règle.
     */
    record SubsidyStats(long activeExemptionsCount, long totalExemptions, long partialExemptions,
                        BigDecimal familyDiscountAmount, int studentsFullySettledMonthlyFees,
                        BigDecimal estimatedEarlyPaymentSavings) {}

    SubsidyStats subsidyStats(Long tenantId, Long academicYearId);

    /** Une échéance de frais (installment) à venir, triée par date limite croissante. */
    record UpcomingInstallment(Long installmentId, Long feeId, String label, LocalDate dueDate, BigDecimal amount) {}

    /** Prochaines échéances de paiement du tenant (statut PENDING) dans les {@code daysAhead}
     * prochains jours, limitées à {@code limit} résultats — pour le dashboard école. */
    List<UpcomingInstallment> upcomingInstallments(Long tenantId, int daysAhead, int limit);
}
