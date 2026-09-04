package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Résout la séance concernée par un pointage (entrée ou sortie), commun élève/enseignant : même
 * règle de correspondance jour/heure sur un emploi du temps hebdomadaire récurrent. */
@Component
class SessionResolver {

    /** La séance que la personne vient assurer/suivre : le 1ᵉʳ cours du jour pas encore terminé à
     * l'heure {@code at} ; sinon le dernier du jour. Null si aucune séance ce jour-là. */
    ScheduleSlotPublicInfo currentSession(List<ScheduleSlotPublicInfo> allSlots, LocalDate date, LocalTime at) {
        List<ScheduleSlotPublicInfo> slots = slotsForDay(allSlots, date);
        if (slots.isEmpty()) return null;
        return slots.stream().filter(s -> LocalTime.parse(s.endTime()).isAfter(at)).findFirst()
                .orElse(slots.get(slots.size() - 1));
    }

    /** Séances du jour (emploi du temps hebdomadaire récurrent), triées par heure de début — sert à
     * détecter deux séances consécutives (marge de transition entre salles, §heures). */
    List<ScheduleSlotPublicInfo> slotsForDay(List<ScheduleSlotPublicInfo> allSlots, LocalDate date) {
        String dayName = frenchDay(date.getDayOfWeek());
        return allSlots.stream()
                .filter(s -> dayName.equalsIgnoreCase(s.day()) && s.startTime() != null && s.endTime() != null)
                .sorted(Comparator.comparing(s -> LocalTime.parse(s.startTime())))
                .toList();
    }

    /** True si l'emploi du temps a au moins une séance ce jour-là (pour le marquage auto d'absence). */
    boolean hasSessionOn(List<ScheduleSlotPublicInfo> allSlots, DayOfWeek day) {
        String dayName = frenchDay(day);
        return allSlots.stream().anyMatch(s -> dayName.equalsIgnoreCase(s.day()));
    }

    private static String frenchDay(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "LUNDI"; case TUESDAY -> "MARDI"; case WEDNESDAY -> "MERCREDI";
            case THURSDAY -> "JEUDI"; case FRIDAY -> "VENDREDI"; case SATURDAY -> "SAMEDI"; case SUNDAY -> "DIMANCHE";
        };
    }
}
