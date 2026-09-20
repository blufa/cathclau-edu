package gn.lacaeducation.academics.domain;

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
public class ClasseMatiere {
    private Long id;
    private Long schoolClassId;
    private Long subjectId;
    private Long teacherId;
    /** Coefficient de la matière pour cette classe (null = coefficient par défaut de la matière). */
    private java.math.BigDecimal coefficient;
    /** Code unique affecté par le Directeur des Études pour identifier ce professeur sur cette
     * matière (ex. "CH1", "CH2" pour le 1er/2e professeur de chimie) — utilisé dans la grille
     * d'emploi du temps pour affecter/réaffecter rapidement un professeur par matière. */
    private String code;
    /** Matière de spécialité de la série (lycée) — ex. Mathématiques en série Scientifique — par
     * opposition à une matière générale du tronc commun. Purement informatif/déclaratif. */
    private boolean specialty;
}
