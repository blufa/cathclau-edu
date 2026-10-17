package gn.lacaeducation.grading;

import java.util.Optional;

/**
 * Nom complet d'un enseignant, pour l'affichage sur les bulletins (ligne "Professeur" sous chaque
 * matière, voir GradeCalculationService). Défini ici, dans le module CONSOMMATEUR, et implémenté par
 * {@code staff} — même pattern que {@link GradingTeacherProvider} dans l'autre sens (staff → grading) :
 * {@code staff} dépend déjà de {@code grading} via ce dernier, donc {@code grading} ne doit jamais
 * importer {@code staff} directement, sous peine de cycle de modules rejeté par ModularityTests.
 */
public interface TeacherNameProvider {
    Optional<String> teacherName(Long teacherId);
}
