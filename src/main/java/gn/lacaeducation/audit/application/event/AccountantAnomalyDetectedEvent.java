package gn.lacaeducation.audit.application.event;

import java.math.BigDecimal;

/**
 * Détecteur B3 (§retour fondateurs) : un comptable dépasse nettement la moyenne de ses pairs sur un
 * indicateur donné — publié UNIQUEMENT à la création (ou réouverture) de l'alerte, jamais à chaque
 * réévaluation hebdomadaire où elle reste active, pour éviter la lassitude aux alertes. Consommé par
 * {@code communication} pour l'alerte SMS (jamais au comptable concerné lui-même).
 */
public record AccountantAnomalyDetectedEvent(
        Long tenantId, Long cashierId, String type, BigDecimal metricValue, BigDecimal peerAverage) {
}
