package gn.lacaeducation.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * API publique du module attendance pour le dashboard école (§reporting) : taux de présence élèves
 * du jour, toutes classes confondues. Un job planifié (AbsenceAutoMarkScheduler, 14h) marque
 * automatiquement absents les élèves non pointés — le pourcentage se stabilise donc en cours de
 * journée, ce n'est pas une donnée figée dès le matin.
 */
public interface StudentAttendanceStatsProvider {

    /** @param expectedCount élèves attendus (inscriptions validées, toutes classes du tenant)
     *  @param presentCount, absentCount, lateCount décompte des présences pointées aujourd'hui
     *  @param presentPercentage presentCount / expectedCount (0 si expectedCount == 0) */
    record TodayAttendanceStats(int expectedCount, int presentCount, int absentCount, int lateCount,
                                BigDecimal presentPercentage) {}

    TodayAttendanceStats todayAttendanceStats(Long tenantId);

    /** Un jour de l'historique — toujours une entrée par jour de la plage demandée, même sans aucun
     * enregistrement ce jour-là (0/0/0), pour que le graphique n'ait jamais de trou. */
    record DailyAttendanceStats(LocalDate date, int presentCount, int absentCount, int lateCount) {}

    /** Historique école entière (tous élèves, toutes classes) des {@code days} derniers jours
     * (aujourd'hui inclus) — tableau de bord principal, §retour fondateur. */
    List<DailyAttendanceStats> attendanceHistory(Long tenantId, int days);
}
