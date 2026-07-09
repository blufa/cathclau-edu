package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "report_cards") @Getter @Setter
public class ReportCardJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "period_id", nullable = false) private Long periodId;
    @Column(name = "overall_average", precision = 5, scale = 2) private BigDecimal overallAverage;
    @Column(name = "class_average", precision = 5, scale = 2) private BigDecimal classAverage;
    @Column(name = "highest_average", precision = 5, scale = 2) private BigDecimal highestAverage;
    @Column(name = "lowest_average", precision = 5, scale = 2) private BigDecimal lowestAverage;
    private Integer rank;
    @Column(name = "class_size") private Integer classSize;
    private String honor;
    private String decision;
    private String comment;
    @Column(name = "pdf_file") private String pdfFile;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportCardStatus status;
    @Column(name = "generated_at") private LocalDateTime generatedAt;
    @Enumerated(EnumType.STRING) @Column(length = 24) private gn.lacaeducation.grading.domain.Distinction distinction;
}
