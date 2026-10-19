package gn.lacaeducation.grading;

import java.time.LocalDate;

/**
 * Absences/retards d'un élève sur une période donnée, pour le bloc BILAN du bulletin annuel (§retour
 * fondateur). Défini ici, dans le module CONSOMMATEUR, et implémenté par {@code attendance} — même
 * pattern que {@link TeacherNameProvider} (staff) : {@code attendance} dépend déjà transitivement de
 * {@code grading} (via {@code staff}, qui dépend de {@link GradingTeacherProvider}), donc {@code grading}
 * ne doit jamais importer {@code attendance} directement, sous peine de cycle de modules
 * (attendance → staff → grading → attendance) rejeté par ModularityTests.
 */
public interface AttendanceSummaryProvider {
    record Summary(int absentCount, int lateCount) {}

    Summary summary(Long studentId, LocalDate from, LocalDate to);
}
