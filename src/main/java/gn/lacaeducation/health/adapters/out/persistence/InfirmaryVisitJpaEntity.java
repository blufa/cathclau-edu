package gn.lacaeducation.health.adapters.out.persistence;

import gn.lacaeducation.health.domain.VisitOutcome;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "infirmary_visits")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class InfirmaryVisitJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "visit_date", nullable = false) private LocalDate visitDate;
    @Column(name = "visit_time") private LocalTime visitTime;
    @Column(columnDefinition = "text") private String reason;
    @Column(name = "care_given", columnDefinition = "text") private String careGiven;
    @Column(precision = 4, scale = 1) private BigDecimal temperature;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private VisitOutcome outcome;
    @Column(name = "parent_notified", nullable = false) private boolean parentNotified;
    @Column(name = "recorded_by") private Long recordedBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
