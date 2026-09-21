package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportCardLine {
    private Long id;
    private Long reportCardId;
    private Long subjectId;
    private BigDecimal subjectAverage;
    private BigDecimal coefficient;
    private BigDecimal weightedAverage;
    /** Moyenne de cours (interrogations/devoirs) de la matière — secondaire uniquement, voir
     * {@code SubjectSemesterAverage} ; {@code null} pour le primaire/maternelle. */
    private BigDecimal coursAverage;
    /** Moyenne de composition de la matière — secondaire uniquement, voir {@code SubjectSemesterAverage} ;
     * {@code null} pour le primaire/maternelle. */
    private BigDecimal compoAverage;
    private Integer subjectRank;
    private String comment;
    private String teacherName;
}
