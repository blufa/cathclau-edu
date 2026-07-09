package gn.lacaeducation.staff.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Charge horaire hebdomadaire d'un enseignant, calculée à partir de ses créneaux d'emploi du temps
 * (§4.1). Valeur agrégée, non persistée : elle reflète l'emploi du temps courant.
 *
 * @param teacherId          l'enseignant concerné
 * @param totalWeeklyMinutes total des minutes d'enseignement sur la semaine
 * @param totalWeeklyHours   total en heures décimales (minutes / 60, 2 décimales)
 * @param slotCount          nombre de créneaux
 * @param minutesByDay       répartition des minutes par jour (clé = libellé du jour)
 */
public record TeacherWorkload(Long teacherId, int totalWeeklyMinutes, BigDecimal totalWeeklyHours,
                              int slotCount, Map<String, Integer> minutesByDay) {

    /** Un créneau minimal (jour + bornes horaires) suffisant au calcul de la charge. */
    public record Slot(String day, LocalTime startTime, LocalTime endTime) {}

    /**
     * Agrège une liste de créneaux en charge hebdomadaire. Les créneaux dont les bornes sont absentes
     * ou incohérentes ({@code end <= start}) sont ignorés. L'ordre d'insertion des jours est conservé.
     */
    public static TeacherWorkload from(Long teacherId, List<Slot> slots) {
        int totalMinutes = 0;
        int count = 0;
        Map<String, Integer> byDay = new LinkedHashMap<>();
        for (Slot s : slots) {
            if (s.startTime() == null || s.endTime() == null
                    || !s.endTime().isAfter(s.startTime())) {
                continue;
            }
            int minutes = (int) Duration.between(s.startTime(), s.endTime()).toMinutes();
            totalMinutes += minutes;
            count++;
            String day = s.day() != null ? s.day() : "?";
            byDay.merge(day, minutes, Integer::sum);
        }
        BigDecimal hours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return new TeacherWorkload(teacherId, totalMinutes, hours, count, byDay);
    }
}
