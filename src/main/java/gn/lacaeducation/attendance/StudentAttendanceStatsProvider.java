package gn.lacaeducation.attendance;

import java.math.BigDecimal;

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
}
