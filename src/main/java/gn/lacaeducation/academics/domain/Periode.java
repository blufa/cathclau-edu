package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
import gn.lacaeducation.academics.domain.valueobject.TypePeriode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Periode {
    private Long id;
    private Long academicYearId;
    private String label;
    private TypePeriode type;
    /** Cycle d'enseignement de la période (primaire = trimestres, secondaire/sup = semestres). */
    private SchoolCycle cycle;
    private Integer sortOrder;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean closed;
    /** Période "composition" associée (§retour fondateur, école Notre Mère) — certaines écoles séparent
     * la période des évaluations "cours" (ex. "1er Semestre") de celle de la composition correspondante
     * (ex. "Compo 1er Semestre"), deux enregistrements distincts mais un seul bulletin. Le moteur de
     * notes fusionne les évaluations des deux périodes lors de la génération ; nul = comportement
     * historique (une seule période par bulletin). */
    private Long compositionPeriodId;
}
