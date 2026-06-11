package gn.lacaeducation.grading.domain;
import lombok.*;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Exam {
    private Long id;
    private Long tenantId;
    private Long classId;
    private Long periodId;
    private ExamType type;
    private String label;
    private LocalDate scheduledDate;
    private String convocationUrl;
    private ExamStatus status;
}
