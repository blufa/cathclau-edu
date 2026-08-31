package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import gn.lacaeducation.academics.domain.valueobject.ScheduleExceptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Exception ponctuelle à un créneau récurrent ({@link EmploiDuTemps}), pour UNE semaine précise
 * seulement (identifiée par son lundi) — ne modifie jamais le créneau récurrent d'origine. Permet de
 * déplacer ({@code MOVED}) ou annuler ({@code CANCELLED}) un cours pour une semaine donnée sans
 * affecter les autres semaines de l'année scolaire.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleException {
    private Long id;
    private Long tenantId;
    /** Créneau récurrent d'origine, jamais modifié par l'exception. */
    private Long originalScheduleId;
    /** Toujours un LUNDI — identifie la semaine concernée. */
    private LocalDate weekStartDate;
    private ScheduleExceptionType type;

    // Renseignés uniquement si type == MOVED (null sinon) :
    private JourSemaine newDay;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    /** null = hérite de la salle du créneau d'origine. */
    private Long newRoomId;
    /** null = hérite de l'enseignant du créneau d'origine. */
    private Long newTeacherId;

    private String reason;
}
