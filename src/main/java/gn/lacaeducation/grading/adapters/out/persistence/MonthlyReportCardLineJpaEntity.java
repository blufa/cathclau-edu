package gn.lacaeducation.grading.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "monthly_report_card_lines") @Getter @Setter
public class MonthlyReportCardLineJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "monthly_report_card_id", nullable = false) private Long monthlyReportCardId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "subject_average", precision = 5, scale = 2) private BigDecimal subjectAverage;
    @Column(precision = 4, scale = 2) private BigDecimal coefficient;
    @Column(name = "teacher_name") private String teacherName;
}
