package gn.lacaeducation.grading.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "monthly_report_cards") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class MonthlyReportCardJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "year_month", nullable = false, length = 7) private String yearMonth;
    @Column(name = "overall_average", precision = 5, scale = 2) private BigDecimal overallAverage;
    @Column(name = "auto_appreciation") private String autoAppreciation;
    @Column(columnDefinition = "text") private String comment;
    @Column(name = "generated_at", nullable = false) private LocalDateTime generatedAt;
}
