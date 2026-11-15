package gn.lacaeducation.academics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Matiere {
    private Long id;
    private Long tenantId;
    private String label;
    private BigDecimal coefficient;
    private String syllabus;
    /** Cycle auquel appartient cette matière (nullable — matières créées avant cette fonctionnalité,
     * ou pertinentes quel que soit le cycle) — §retour utilisateur : "les mêmes matières s'affichent
     * à tous les niveaux", sert à filtrer l'affichage et les affectations par niveau. */
    private gn.lacaeducation.academics.domain.valueobject.SchoolCycle cycle;
}
