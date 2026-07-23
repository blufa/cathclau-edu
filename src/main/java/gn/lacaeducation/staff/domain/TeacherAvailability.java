package gn.lacaeducation.staff.domain;

import lombok.*;

import java.time.LocalTime;

/** Plage de disponibilité d'un enseignant un jour donné (ex. Lundi 08:00–12:00). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherAvailability {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private String day;          // LUNDI..SAMEDI
    private LocalTime startTime;
    private LocalTime endTime;
    /** Classe et matière concernées par cette disponibilité (obligatoires à la saisie — voir
     * TeacherAvailabilityService#add) : un enseignant ne déclare pas un créneau ouvert mais une
     * disponibilité pour enseigner une matière donnée à une classe donnée. */
    private Long schoolClassId;
    private Long subjectId;
}
