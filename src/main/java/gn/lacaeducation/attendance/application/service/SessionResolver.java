package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.attendance.domain.RecessWindow;
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

    /** True si au moins une séance du jour a déjà commencé à l'heure {@code at} — utilisé par le
     * marquage auto d'absence de mi-journée (14h) pour ne pas signaler à tort un élève dont le seul
     * cours du jour est l'après-midi et n'a pas encore débuté. */
    boolean hasStartedSessionOn(List<ScheduleSlotPublicInfo> allSlots, LocalDate date, LocalTime at) {
        return slotsForDay(allSlots, date).stream()
                .anyMatch(s -> !LocalTime.parse(s.startTime()).isAfter(at));
    }

    /**
     * Heure à partir de laquelle un retard commence à être compté pour une entrée prévue à
     * {@code start} : {@code start} repoussée (a) par la marge de tolérance générale configurée par
     * l'école ({@code marginMinutes} — ex. 5 min, s'applique à TOUTE entrée, pas seulement en cas de
     * changement de salle) et/ou (b) si elle tombe dans une fenêtre de récréation (temps non
     * imputable à la personne). Commun élève/enseignant/personnel — la même tolérance vaut pour tous.
     */
    LocalTime graceDeadline(LocalTime start, int marginMinutes, List<RecessWindow> recessWindows) {
        LocalTime deadline = start;
        for (RecessWindow w : recessWindows) {
            if (w.contains(start) && w.getEndTime().isAfter(deadline)) deadline = w.getEndTime();
        }
        LocalTime withMargin = start.plusMinutes(marginMinutes);
        return withMargin.isAfter(deadline) ? withMargin : deadline;
    }

    private static String frenchDay(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "LUNDI"; case TUESDAY -> "MARDI"; case WEDNESDAY -> "MERCREDI";
            case THURSDAY -> "JEUDI"; case FRIDAY -> "VENDREDI"; case SATURDAY -> "SAMEDI"; case SUNDAY -> "DIMANCHE";
        };
    }
}
