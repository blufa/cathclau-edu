package gn.lacaeducation.audit.domain;

/**
 * Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable) : signal statistique comparant
 * un comptable à ses pairs du même établissement, sur une fenêtre glissante de 30 jours.
 */
public enum AccountantAnomalyType {
    /** Somme des écarts de caisse négatifs (manquants) à la clôture, anormalement élevée par
     * rapport aux pairs — voir {@code AccountantMetricsPort#cashDiscrepancyMetrics}. */
    CASH_DISCREPANCY,
    /** Nombre d'exonérations accordées, anormalement élevé par rapport aux pairs — voir
     * {@code AccountantMetricsPort#exemptionVolumeMetrics}. Le nombre (pas le montant) est utilisé
     * car une exonération en pourcentage (TOTAL/PARTIAL) n'a pas de valeur monétaire propre tant
     * qu'elle n'est pas appliquée à une facture précise. */
    EXEMPTION_VOLUME
}
