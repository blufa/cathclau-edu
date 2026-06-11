package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.domain.ExamStatus;
import gn.lacaeducation.grading.domain.ExamType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name = "exams") @Getter @Setter
public class ExamJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "period_id", nullable = false) private Long periodId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ExamType type;
    @Column(nullable = false) private String label;
    @Column(name = "scheduled_date") private LocalDate scheduledDate;
    @Column(name = "convocation_url") private String convocationUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ExamStatus status;
}
