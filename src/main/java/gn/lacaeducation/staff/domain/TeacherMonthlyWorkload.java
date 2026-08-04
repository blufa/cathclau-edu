package gn.lacaeducation.staff.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Charge horaire mensuelle d'un enseignant (§2 grille horaire), dérivée de son emploi du temps
 * hebdomadaire récurrent : chaque créneau est compté autant de fois que son jour de la semaine
 * apparaît dans le mois demandé (ex. un créneau du lundi compte pour le nombre de lundis du mois).
 * Valeur agrégée, non persistée — alimente le compteur d'heures mensuelles (et, plus tard, la paie).
 *
 * @param sessionCount nombre total de séances dans le mois (occurrences de créneaux, tous jours confondus)
 */
public record TeacherMonthlyWorkload(Long teacherId, int year, int month, int totalMonthlyMinutes,
                                      BigDecimal totalMonthlyHours, int sessionCount) {

    private static final Map<String, DayOfWeek> DAY_OF_WEEK = Map.of(
            "LUNDI", DayOfWeek.MONDAY, "MARDI", DayOfWeek.TUESDAY, "MERCREDI", DayOfWeek.WEDNESDAY,
            "JEUDI", DayOfWeek.THURSDAY, "VENDREDI", DayOfWeek.FRIDAY, "SAMEDI", DayOfWeek.SATURDAY,
            "DIMANCHE", DayOfWeek.SUNDAY);

    public static TeacherMonthlyWorkload from(Long teacherId, int year, int month, List<TeacherWorkload.Slot> slots) {
        YearMonth ym = YearMonth.of(year, month);
        Map<DayOfWeek, Integer> occurrencesByDay = new EnumMap<>(DayOfWeek.class);
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            occurrencesByDay.merge(ym.atDay(day).getDayOfWeek(), 1, Integer::sum);
        }

        int totalMinutes = 0;
        int sessionCount = 0;
        for (TeacherWorkload.Slot slot : slots) {
            if (slot.startTime() == null || slot.endTime() == null || !slot.endTime().isAfter(slot.startTime())) {
                continue;
            }
            DayOfWeek dayOfWeek = slot.day() != null ? DAY_OF_WEEK.get(slot.day().toUpperCase()) : null;
            if (dayOfWeek == null) continue;
            int occurrences = occurrencesByDay.getOrDefault(dayOfWeek, 0);
            int minutes = (int) Duration.between(slot.startTime(), slot.endTime()).toMinutes();
            totalMinutes += minutes * occurrences;
            sessionCount += occurrences;
        }
        BigDecimal hours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return new TeacherMonthlyWorkload(teacherId, year, month, totalMinutes, hours, sessionCount);
    }
}
