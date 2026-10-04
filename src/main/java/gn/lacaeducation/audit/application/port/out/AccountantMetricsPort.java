package gn.lacaeducation.audit.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lecture agrégée, par comptable, des deux signaux du détecteur B3 (§retour fondateurs) — miroirs
 * en lecture seule de tables de {@code payments}/{@code billing} (même principe que
 * {@code attendance.EnrollmentQueryPort} sur la table {@code enrollments}), pour ne pas dépendre
 * des beans internes de ces modules.
 */
public interface AccountantMetricsPort {

    record CashDiscrepancyMetric(Long cashierId, BigDecimal missingTotal, long sessionCount) {}

    record ExemptionVolumeMetric(Long cashierId, long exemptionCount) {}

    /** Somme des écarts négatifs (manquants) des sessions de caisse CLOSED de chaque comptable,
     * depuis {@code since}, avec le nombre de sessions prises en compte (pour le seuil d'éligibilité
     * — un comptable avec trop peu de sessions ne doit pas être comparé). */
    List<CashDiscrepancyMetric> cashDiscrepancyMetrics(Long tenantId, LocalDateTime since);

    /** Nombre d'exonérations accordées par chaque comptable depuis {@code since}. */
    List<ExemptionVolumeMetric> exemptionVolumeMetrics(Long tenantId, LocalDateTime since);
}
