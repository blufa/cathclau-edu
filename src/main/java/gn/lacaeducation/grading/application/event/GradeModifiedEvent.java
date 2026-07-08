package gn.lacaeducation.grading.application.event;

import java.math.BigDecimal;

/**
 * Publié quand un enseignant modifie la note d'un élève (correction). Consommé par communication
 * pour alerter par email l'administration de l'école (ADMIN_ECOLE) et le responsable pédagogique,
 * avec le motif obligatoire de la modification.
 */
public record GradeModifiedEvent(Long tenantId, Long gradeId, Long assessmentId, Long studentId,
                                 BigDecimal oldValue, BigDecimal newValue, Long changedBy, String reason) {}
