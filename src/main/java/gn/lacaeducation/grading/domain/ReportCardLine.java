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
    private Integer subjectRank;
    private String comment;
    private String teacherName;
}
