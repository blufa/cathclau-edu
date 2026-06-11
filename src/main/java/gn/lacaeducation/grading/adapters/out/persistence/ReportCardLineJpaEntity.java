package gn.lacaeducation.grading.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "report_card_lines") @Getter @Setter
public class ReportCardLineJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "report_card_id", nullable = false) private Long reportCardId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "subject_average", precision = 5, scale = 2) private BigDecimal subjectAverage;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal coefficient;
    @Column(name = "weighted_average", precision = 5, scale = 2) private BigDecimal weightedAverage;
    @Column(name = "subject_rank") private Integer subjectRank;
    private String comment;
    @Column(name = "teacher_name") private String teacherName;
}
