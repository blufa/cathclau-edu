package gn.lacaeducation.attendance;

/**
 * API publique du module attendance pour la paie : heures de travail mensuelles d'un enseignant,
 * nettes des absences et des retards (calculés par séance au pointage).
 */
public interface TeacherWorkProvider {
    /** Minutes du mois : dues (emploi du temps), de retard cumulé, de départ anticipé cumulé,
     * d'absence, et effectives (= dues − absences − retards − départs anticipés). */
    record MonthlyWork(int dueMinutes, int lateMinutes, int earlyMinutes, int absentMinutes, int effectiveMinutes,
                       long presentDays, long absentDays) {}

    MonthlyWork monthlyWork(Long teacherId, int year, int month);
}
