package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyReportCardLine {
    private Long id;
    private Long monthlyReportCardId;
    private Long subjectId;
    private BigDecimal subjectAverage;
    private BigDecimal coefficient;
    private String teacherName;
}
