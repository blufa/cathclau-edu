package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportCard {
    private Long id;
    private Long studentId;
    private Long classId;
    private Long periodId;
    private BigDecimal overallAverage;
    private BigDecimal classAverage;
    private BigDecimal highestAverage;
    private BigDecimal lowestAverage;
    private Integer rank;
    private Integer classSize;
    private String honor;
    private String decision;
    private String comment;
    private String pdfFile;
    private ReportCardStatus status;
    private LocalDateTime generatedAt;
    /** Appréciation du conseil de classe (Félicitations, Compliments, Encouragements…). */
    private Distinction distinction;
    /** Assiduité sur la période du bulletin (§retour fondateur, "bulletin-test") — null = non calculé
     * (bulletins générés avant cette fonctionnalité). */
    private Integer absentCount;
    private Integer lateCount;
}
