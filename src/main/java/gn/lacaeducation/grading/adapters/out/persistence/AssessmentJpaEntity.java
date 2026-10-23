package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.domain.AssessmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name = "assessments") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class AssessmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
    @Column(name = "period_id", nullable = false) private Long periodId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AssessmentType type;
    @Column(name = "assessment_type_id") private Long assessmentTypeId;
    @Column(nullable = false) private String label;
    @Column(nullable = false) private LocalDate date;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal coefficient;
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2) private BigDecimal maxScore;
    @Column(name = "exam_id") private Long examId;
}
