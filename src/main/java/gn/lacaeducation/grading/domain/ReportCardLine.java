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
    /** Moyenne de cours de la matière (étape de formule étiquetée COURS), si la formule active en fournit une. */
    private BigDecimal coursAverage;
    /** Moyenne de composition de la matière (étape de formule étiquetée COMPOSITION), si la formule active en fournit une. */
    private BigDecimal compoAverage;
    private Integer subjectRank;
    private String comment;
    private String teacherName;
}
