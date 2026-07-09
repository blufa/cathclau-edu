package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.GradeAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grade_history")
@Getter @Setter
public class GradeHistoryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "grade_id") private Long gradeId;
    @Column(name = "assessment_id", nullable = false) private Long assessmentId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private GradeAction action;
    @Column(name = "old_value", precision = 5, scale = 2) private BigDecimal oldValue;
    @Column(name = "new_value", precision = 5, scale = 2) private BigDecimal newValue;
    @Column(name = "changed_by") private Long changedBy;
    @Column(length = 500) private String reason;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
}
