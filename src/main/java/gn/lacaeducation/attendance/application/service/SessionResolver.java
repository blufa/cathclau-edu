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

    /** Un ensemble de créneaux consécutifs (écart entre deux créneaux ≤ la marge de transition
     * configurée) formant une seule plage de présence continue — un enseignant/élève avec par exemple
     * 8h-10h puis 10h-12h puis 12h-14h ne doit scanner qu'une fois à l'entrée et une fois à la sortie
     * pour toute la plage, pas un aller-retour par créneau (§retour direction pédagogique). */
    record Block(List<ScheduleSlotPublicInfo> slots) {
        Long firstSlotId() { return slots.get(0).id(); }
        LocalTime start() { return LocalTime.parse(slots.get(0).startTime()); }
        LocalTime end() { return LocalTime.parse(slots.get(slots.size() - 1).endTime()); }
    }

    /** Le bloc de créneaux continus que la personne vient assurer/suivre à l'heure {@code at}
     * (équivalent de {@link #currentSession}, mais fusionne les créneaux consécutifs en une seule
     * plage). Null si aucune séance ce jour-là. */
    Block currentBlock(List<ScheduleSlotPublicInfo> allSlots, LocalDate date, LocalTime at, int marginMinutes) {
        List<List<ScheduleSlotPublicInfo>> blocks = groupIntoBlocks(slotsForDay(allSlots, date), marginMinutes);
        if (blocks.isEmpty()) return null;
        return blocks.stream()
                .filter(b -> LocalTime.parse(b.get(b.size() - 1).endTime()).isAfter(at))
                .findFirst()
                .map(Block::new)
                .orElseGet(() -> new Block(blocks.get(blocks.size() - 1)));
    }

    /** Regroupe des créneaux déjà triés par heure de début en blocs de créneaux consécutifs — deux
     * créneaux appartiennent au même bloc si l'écart entre la fin de l'un et le début du suivant ne
     * dépasse pas {@code marginMinutes} (la même marge de transition que la tolérance de retard,
     * réutilisée ici comme écart maximal pour rester « une seule séance continue »). */
    private List<List<ScheduleSlotPublicInfo>> groupIntoBlocks(List<ScheduleSlotPublicInfo> sortedSlots, int marginMinutes) {
        List<List<ScheduleSlotPublicInfo>> blocks = new java.util.ArrayList<>();
        List<ScheduleSlotPublicInfo> current = new java.util.ArrayList<>();
        LocalTime prevEnd = null;
        for (ScheduleSlotPublicInfo s : sortedSlots) {
            LocalTime start = LocalTime.parse(s.startTime());
            if (prevEnd != null && start.isAfter(prevEnd.plusMinutes(marginMinutes))) {
                blocks.add(current);
                current = new java.util.ArrayList<>();
            }
            current.add(s);
            prevEnd = LocalTime.parse(s.endTime());
        }
        if (!current.isEmpty()) blocks.add(current);
        return blocks;
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
