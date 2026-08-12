package gn.lacaeducation.grading.domain;

import lombok.*;

import java.util.List;

/**
 * Bulletin annuel d'un élève (§ documents ministère) : agrège à la volée les deux bulletins
 * semestriels (1er et 2ème semestre) d'une même année scolaire — jamais persisté (§CLAUDE.md, bonus
 * « bulletin annuel multi-périodes », pure agrégation de l'existant). La moyenne générale annuelle
 * est la moyenne simple des deux moyennes générales semestrielles ; le rang est recalculé à la volée
 * sur l'ensemble des élèves de la classe demandée.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnualReportCard {
    private Long studentId;
    private Long classId;
    private Long academicYearId;
    private Long s1PeriodId;
    private Long s2PeriodId;
    private List<AnnualReportCardLine> lines;
    private java.math.BigDecimal s1Average;
    private java.math.BigDecimal s2Average;
    private java.math.BigDecimal overallAverage;
    private Integer rank;
    private Integer classSize;
    private String honor;
    private String decision;
}
