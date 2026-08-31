package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
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
public class Niveau {
    private Long id;
    private Long tenantId;
    private String label;
    private Integer sortOrder;
    /** Cycle scolaire (maternelle/primaire/collège/lycée/supérieur) — nullable, à renseigner par
     * l'école ; sert notamment à déclencher le badge de retrait automatique à la sortie (§attendance). */
    private SchoolCycle cycle;
}
