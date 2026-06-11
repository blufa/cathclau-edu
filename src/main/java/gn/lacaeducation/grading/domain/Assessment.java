package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Assessment {
    private Long id;
    private Long tenantId;
    private Long classId;
    private Long subjectId;
    private Long teacherId;
    private Long periodId;
    private AssessmentType type;
    private String label;
    private LocalDate date;
    private BigDecimal coefficient;
    private BigDecimal maxScore;
}
