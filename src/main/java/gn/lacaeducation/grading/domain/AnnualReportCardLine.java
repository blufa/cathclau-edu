package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne (matière) d'un {@link AnnualReportCard} : reprend les moyennes de cours/composition/matière
 * des deux semestres, et la moyenne annuelle de la matière (moyenne simple des deux semestres, §5).
 * Calculée à la volée à partir des {@link ReportCardLine} des deux périodes — aucune persistance.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnualReportCardLine {
    private Long subjectId;
    private BigDecimal coefficient;
    private BigDecimal s1Cours;
    private BigDecimal s1Compo;
    private BigDecimal s1Average;
    private BigDecimal s2Cours;
    private BigDecimal s2Compo;
    private BigDecimal s2Average;
    private BigDecimal annualAverage;
}
