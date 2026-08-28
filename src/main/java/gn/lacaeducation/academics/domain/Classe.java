package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.ClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Classe {
    private Long id;
    private Long tenantId;
    private Long levelId;
    private Long academicYearId;
    private Long headTeacherId;
    private Long roomId;
    private String label;
    private Integer headcount;
    private Integer maxCapacity;
    /** Classe normale ou classe d'examen (§6) — détermine le tarif horaire de paie appliqué. */
    @Builder.Default
    private ClassType classType = ClassType.NORMALE;
    /** Élève désigné (inscription validée) pour scanner les badges de sa classe le matin — géré via
     * un endpoint dédié, jamais via la mise à jour générale de la classe. */
    private Long classRepresentativeStudentId;
}
