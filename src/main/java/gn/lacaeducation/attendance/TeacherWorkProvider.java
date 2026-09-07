package gn.lacaeducation.attendance;

import java.util.Map;

/**
 * API publique du module attendance pour la paie : heures de travail mensuelles d'un enseignant,
 * nettes des absences et des retards (calculés par séance au pointage).
 */
public interface TeacherWorkProvider {
    /** Minutes du mois : dues (emploi du temps), de retard cumulé, de départ anticipé cumulé,
     * d'absence, et effectives (= dues − absences − retards − départs anticipés). */
    record MonthlyWork(int dueMinutes, int lateMinutes, int earlyMinutes, int absentMinutes, int effectiveMinutes,
                       long presentDays, long absentDays) {}

    /** Retard + départ anticipé cumulés (minutes) pour une classe donnée sur le mois. */
    record ClassLateEarly(int lateMinutes, int earlyMinutes) {}

    MonthlyWork monthlyWork(Long teacherId, int year, int month);

    /** Retard/départ anticipé cumulés du mois, par classe (clé = classId) — permet à la paie
     * d'appliquer le tarif horaire correspondant à la typologie de chaque classe (normal/examen)
     * sur les heures réellement travaillées, au lieu d'un tarif unique. */
    Map<Long, ClassLateEarly> monthlyLateEarlyByClass(Long teacherId, int year, int month);
}
