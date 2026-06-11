package gn.lacaeducation.grading.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "grades") @Getter @Setter
public class GradeJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "assessment_id", nullable = false) private Long assessmentId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal value;
    @Column(nullable = false) private Boolean validated;
    @Column(name = "entered_at", nullable = false) private LocalDateTime enteredAt;
}
