package gn.lacaeducation.attendance.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Retrait d'un enfant (maternelle/primaire) : qui est venu le récupérer à la sortie, pour la preuve
 * imprimable (badge de retrait) et l'historique. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChildPickup {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private LocalDate pickupDate;
    private LocalTime pickupTime;
    /** Rempli si un parent/tuteur déjà lié à l'élève a été choisi ; null pour « Autre personne ». */
    private Long pickerParentId;
    /** Toujours renseigné (nom du parent choisi recopié, ou saisie libre) — imprimé sur le badge. */
    private String pickerName;
    /** Lien avec l'enfant (Père/Mère/Tuteur/Autre), saisi librement. */
    private String pickerRelation;
    private LocalDateTime createdAt;
}
