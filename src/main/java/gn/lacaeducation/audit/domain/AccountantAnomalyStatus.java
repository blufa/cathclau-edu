package gn.lacaeducation.audit.domain;

/** Pas de transition manuelle via API dans cette itération : géré uniquement par
 * {@code AccountantAnomalyAlertService}, qui réouvre/résout automatiquement à chaque réévaluation
 * hebdomadaire selon que le comptable dépasse encore le seuil ou pas. */
public enum AccountantAnomalyStatus {
    ACTIVE, RESOLVED
}
