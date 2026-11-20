package gn.lacaeducation.grading;

import java.util.Optional;

/**
 * Résout la fiche enseignant liée à un compte utilisateur connecté — pour cloisonner la saisie des
 * notes/évaluations par propriétaire (voir {@code AssessmentOwnershipGuard}). Défini ici, dans le
 * module CONSOMMATEUR, et implémenté par {@code staff} — même pattern que {@link TeacherNameProvider}
 * : {@code staff} dépend déjà de {@code grading} via {@link GradingTeacherProvider}, donc
 * {@code grading} ne doit jamais importer {@code staff} directement, sous peine de cycle de modules
 * rejeté par ModularityTests.
 */
public interface TeacherOwnershipLookup {
    Optional<Long> findTeacherIdByUserId(Long userId);
}
