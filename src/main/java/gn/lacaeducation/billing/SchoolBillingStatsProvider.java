package gn.lacaeducation.billing;

import java.math.BigDecimal;

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
}
